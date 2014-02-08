package com.thoughtworks.i0.container.grizzly.internal.tryus;

import org.glassfish.tyrus.core.*;
import org.glassfish.tyrus.core.frame.CloseFrame;
import org.glassfish.tyrus.core.uri.Match;
import org.glassfish.tyrus.spi.*;

import javax.websocket.*;
import javax.websocket.server.ServerEndpointConfig;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TyrusWebSocketEngine implements WebSocketEngine {

    public static final String INCOMING_BUFFER_SIZE = "org.glassfish.tyrus.incomingBufferSize";

    private static final int BUFFER_STEP_SIZE = 256;
    private static final Logger LOGGER = Logger.getLogger(UpgradeRequest.WEBSOCKET);

    private static final UpgradeInfo NOT_APPLICABLE_UPGRADE_INFO =
            new NoConnectionUpgradeInfo(UpgradeStatus.NOT_APPLICABLE);

    private static final UpgradeInfo HANDSHAKE_FAILED_UPGRADE_INFO =
            new NoConnectionUpgradeInfo(UpgradeStatus.HANDSHAKE_FAILED);


    private final Set<TyrusEndpoint> endpoints = Collections.newSetFromMap(new ConcurrentHashMap<TyrusEndpoint, Boolean>());
    private final ComponentProviderService componentProviderService = ComponentProviderService.create();
    private final WebSocketContainer webSocketContainer;

    private int incomingBufferSize = 4194315; // 4M (payload) + 11 (frame overhead)

    /**
     * Create {@link WebSocketEngine} instance based on passed {@link WebSocketContainer}.
     *
     * @param webSocketContainer used {@link WebSocketContainer} instance.
     */
    public TyrusWebSocketEngine(WebSocketContainer webSocketContainer) {
        this(webSocketContainer, null);
    }

    /**
     * Create {@link WebSocketEngine} instance based on passed {@link WebSocketContainer} and with configured maximal
     * incoming buffer size.
     *
     * @param webSocketContainer used {@link WebSocketContainer} instance.
     * @param incomingBufferSize maximal incoming buffer size (this engine won't be able to process messages bigger
     *                           than this number. If null, default value will be used).
     */
    public TyrusWebSocketEngine(WebSocketContainer webSocketContainer, Integer incomingBufferSize) {
        if (incomingBufferSize != null) {
            this.incomingBufferSize = incomingBufferSize;
        }
        this.webSocketContainer = webSocketContainer;
    }

    private static ProtocolHandler loadHandler(UpgradeRequest request) {
        for (Version version : Version.values()) {
            if (version.validate(request)) {
                return version.createHandler(false);
            }
        }
        return null;
    }

    private static void handleUnsupportedVersion(final UpgradeRequest request, UpgradeResponse response) {
        response.setStatus(426);
        response.getHeaders().put(UpgradeRequest.SEC_WEBSOCKET_VERSION,
                Arrays.asList(Version.getSupportedWireProtocolVersions()));
    }

    TyrusEndpoint getEndpoint(UpgradeRequest request) {
        if (endpoints.isEmpty()) {
            return null;
        }

        final String requestPath = request.getRequestUri();

        for (Match m : Match.getAllMatches(requestPath, endpoints)) {
            final TyrusEndpoint endpoint = m.getTyrusEndpoit();

            for (String name : m.getParameterNames()) {
                request.getParameterMap().put(name, Arrays.asList(m.getParameterValue(name)));
            }

            if (endpoint.upgrade(request)) {
                return endpoint;
            }
        }

        return null;
    }

    @Override
    public UpgradeInfo upgrade(final UpgradeRequest request, final UpgradeResponse response) {

        try {
            final TyrusEndpoint endpoint = getEndpoint(request);
            if (endpoint != null) {
                final ProtocolHandler protocolHandler = loadHandler(request);
                if (protocolHandler == null) {
                    handleUnsupportedVersion(request, response);
                    return HANDSHAKE_FAILED_UPGRADE_INFO;
                }

                final ExtendedExtension.ExtensionContext extensionContext = new ExtendedExtension.ExtensionContext() {

                    private final Map<String, Object> properties = new HashMap<String, Object>();

                    @Override
                    public Map<String, Object> getProperties() {
                        return properties;
                    }
                };

                protocolHandler.handshake(endpoint, request, response, extensionContext);
                return new SuccessfulUpgradeInfo(endpoint, protocolHandler, incomingBufferSize, request, extensionContext);
            }
        } catch (HandshakeException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
            response.setStatus(e.getCode());
            return HANDSHAKE_FAILED_UPGRADE_INFO;
        }

        response.setStatus(500);
        return NOT_APPLICABLE_UPGRADE_INFO;
    }

    private static class TyrusReadHandler implements ReadHandler {

        private final ProtocolHandler protocolHandler;
        private final TyrusWebSocket socket;
        private final TyrusEndpoint endpoint;
        private final int incomingBufferSize;
        private final List<Extension> negotiatedExtensions;
        private final ExtendedExtension.ExtensionContext extensionContext;

        private volatile ByteBuffer buffer;

        private TyrusReadHandler(ProtocolHandler protocolHandler, TyrusWebSocket socket, TyrusEndpoint endpoint, int incomingBufferSize, ExtendedExtension.ExtensionContext extensionContext) {
            this.extensionContext = extensionContext;
            this.protocolHandler = protocolHandler;
            this.socket = socket;
            this.endpoint = endpoint;
            this.incomingBufferSize = incomingBufferSize;
            this.negotiatedExtensions = new ArrayList<Extension>();
            negotiatedExtensions.addAll(endpoint.getSupportedExtensions());
        }

        @Override
        public void handle(ByteBuffer data) {
            try {
                if (data != null && data.hasRemaining()) {

                    if (buffer != null) {
                        data = Utils.appendBuffers(buffer, data, incomingBufferSize, BUFFER_STEP_SIZE);
                    } else {
                        int newSize = data.remaining();
                        if (newSize > incomingBufferSize) {
                            throw new IllegalArgumentException("Buffer overflow.");
                        } else {
                            final int roundedSize = (newSize % BUFFER_STEP_SIZE) > 0 ? ((newSize / BUFFER_STEP_SIZE) + 1) * BUFFER_STEP_SIZE : newSize;
                            final ByteBuffer result = ByteBuffer.allocate(roundedSize > incomingBufferSize ? newSize : roundedSize);
                            result.flip();
                            data = Utils.appendBuffers(result, data, incomingBufferSize, BUFFER_STEP_SIZE);
                        }
                    }

                    do {
                        final Frame incomingFrame = protocolHandler.unframe(data);

                        if (incomingFrame == null) {
                            buffer = data;
                            break;
                        } else {
                            Frame frame = incomingFrame;

                            for (Extension extension : negotiatedExtensions) {
                                if (extension instanceof ExtendedExtension) {
                                    try {
                                        frame = ((ExtendedExtension) extension).processIncoming(extensionContext, frame);
                                    } catch (Throwable t) {
                                        LOGGER.log(Level.FINE, String.format("Extension '%s' threw an exception during processIncoming method invocation: \"%s\".", extension.getName(), t.getMessage()), t);
                                    }
                                }
                            }

                            protocolHandler.process(frame, socket);
                        }
                    } while (true);
                }
            } catch (FramingException e) {
                LOGGER.log(Level.FINE, e.getMessage(), e);
                socket.onClose(new CloseFrame(new CloseReason(CloseReason.CloseCodes.getCloseCode(e.getClosingCode()), e.getMessage())));
            } catch (Exception e) {
                LOGGER.log(Level.FINE, e.getMessage(), e);
                if (endpoint.onError(socket, e)) {
                    socket.onClose(new CloseFrame(new CloseReason(CloseReason.CloseCodes.UNEXPECTED_CONDITION, e.getMessage())));
                }
            }
        }
    }

    public void setIncomingBufferSize(int incomingBufferSize) {
        this.incomingBufferSize = incomingBufferSize;
    }

    /**
     * Registers the specified {@link TyrusEndpoint} with the
     * <code>WebSocketEngine</code>.
     *
     * @param endpoint the {@link TyrusEndpoint} to register.
     * @throws javax.websocket.DeploymentException when added endpoint responds to same path as some already registered endpoint.
     */
    private void register(TyrusEndpoint endpoint) throws DeploymentException {
        checkPath(endpoint);
        endpoints.add(endpoint);
    }

    public void register(AnnotatedEndpoint endpoint, String contextPath) throws DeploymentException {
        final ErrorCollector collector = new ErrorCollector();
        EndpointConfig config = endpoint.getEndpointConfig();
        TyrusEndpointWrapper ew = new TyrusEndpointWrapper(endpoint, config, componentProviderService, webSocketContainer,
                contextPath, config instanceof ServerEndpointConfig ? ((ServerEndpointConfig) config).getConfigurator() : null);
        if (collector.isEmpty()) {
            register(new TyrusEndpoint(ew));
        } else {
            throw collector.composeComprehensiveException();
        }
    }

    @Override
    public void register(Class<?> endpointClass, String contextPath) throws DeploymentException {

        final ErrorCollector collector = new ErrorCollector();

        AnnotatedEndpoint endpoint = AnnotatedEndpoint.fromClass(endpointClass, componentProviderService, true, collector);
        EndpointConfig config = endpoint.getEndpointConfig();

        TyrusEndpointWrapper ew = new TyrusEndpointWrapper(endpoint, config, componentProviderService, webSocketContainer,
                contextPath, config instanceof ServerEndpointConfig ? ((ServerEndpointConfig) config).getConfigurator() : null);

        if (collector.isEmpty()) {
            register(new TyrusEndpoint(ew));
        } else {
            throw collector.composeComprehensiveException();
        }
    }

    @Override
    public void register(ServerEndpointConfig serverConfig, String contextPath) throws DeploymentException {

        TyrusEndpointWrapper ew;

        Class<?> endpointClass = serverConfig.getEndpointClass();
        boolean isEndpointClass = false;

        do {
            endpointClass = endpointClass.getSuperclass();
            if (endpointClass.equals(Endpoint.class)) {
                isEndpointClass = true;
            }
        } while (!endpointClass.equals(Object.class));

        if (isEndpointClass) {
            // we are pretty sure that endpoint class is javax.websocket.Endpoint descendant.
            //noinspection unchecked
            ew = new TyrusEndpointWrapper((Class<? extends Endpoint>) serverConfig.getEndpointClass(),
                    serverConfig, componentProviderService, webSocketContainer, contextPath, serverConfig.getConfigurator());
        } else {
            final ErrorCollector collector = new ErrorCollector();

            final AnnotatedEndpoint endpoint = AnnotatedEndpoint.fromClass(serverConfig.getEndpointClass(), componentProviderService, true, collector);
            final EndpointConfig config = endpoint.getEndpointConfig();

            ew = new TyrusEndpointWrapper(endpoint, config, componentProviderService, webSocketContainer,
                    contextPath, config instanceof ServerEndpointConfig ? ((ServerEndpointConfig) config).getConfigurator() : null);

            if (!collector.isEmpty()) {
                throw collector.composeComprehensiveException();
            }
        }

        register(new TyrusEndpoint(ew));
    }

    private void checkPath(TyrusEndpoint endpoint) throws DeploymentException {
        for (TyrusEndpoint tyrusEndpoint : endpoints) {
            if (Match.isEquivalent(endpoint.getPath(), tyrusEndpoint.getPath())) {
                throw new DeploymentException(String.format(
                        "Found equivalent paths. Added path: '%s' is equivalent with '%s'.", endpoint.getPath(),
                        tyrusEndpoint.getPath()));
            }
        }
    }

    /**
     * Un-registers the specified {@link TyrusEndpoint} with the
     * <code>WebSocketEngine</code>.
     *
     * @param endpoint the {@link TyrusEndpoint} to un-register.
     */
    public void unregister(TyrusEndpoint endpoint) {
        endpoints.remove(endpoint);
    }

    private static class NoConnectionUpgradeInfo implements UpgradeInfo {
        private final UpgradeStatus status;

        NoConnectionUpgradeInfo(UpgradeStatus status) {
            this.status = status;
        }

        @Override
        public UpgradeStatus getStatus() {
            return status;
        }

        @Override
        public Connection createConnection(Writer writer, Connection.CloseListener closeListener) {
            return null;
        }
    }

    private static class SuccessfulUpgradeInfo implements UpgradeInfo {

        private final TyrusEndpoint endpoint;
        private final ProtocolHandler protocolHandler;
        private final int incomingBufferSize;
        private final UpgradeRequest upgradeRequest;
        private final ExtendedExtension.ExtensionContext extensionContext;

        SuccessfulUpgradeInfo(TyrusEndpoint endpoint, ProtocolHandler protocolHandler, int incomingBufferSize, UpgradeRequest upgradeRequest, ExtendedExtension.ExtensionContext extensionContext) {
            this.endpoint = endpoint;
            this.protocolHandler = protocolHandler;
            this.incomingBufferSize = incomingBufferSize;
            this.upgradeRequest = upgradeRequest;
            this.extensionContext = extensionContext;
        }

        @Override
        public UpgradeStatus getStatus() {
            return UpgradeStatus.SUCCESS;
        }

        @Override
        public Connection createConnection(Writer writer, Connection.CloseListener closeListener) {
            return new TyrusConnection(endpoint, protocolHandler, incomingBufferSize, writer, closeListener, upgradeRequest, extensionContext);
        }
    }

    static class TyrusConnection implements Connection {

        private final ReadHandler readHandler;
        private final Writer writer;
        private final CloseListener closeListener;
        private final TyrusWebSocket socket;
        private final ExtendedExtension.ExtensionContext extensionContext;
        private final TyrusEndpoint endpoint;

        TyrusConnection(TyrusEndpoint endpoint, ProtocolHandler protocolHandler, int incomingBufferSize, Writer writer, Connection.CloseListener closeListener, UpgradeRequest upgradeRequest, ExtendedExtension.ExtensionContext extensionContext) {
            protocolHandler.setWriter(writer);
            final TyrusWebSocket socket = endpoint.createSocket(protocolHandler);

            socket.onConnect(upgradeRequest);
            this.socket = socket;
            this.readHandler = new TyrusReadHandler(protocolHandler, socket, endpoint, incomingBufferSize, extensionContext);
            this.writer = writer;
            this.closeListener = closeListener;
            this.extensionContext = extensionContext;
            this.endpoint = endpoint;
        }

        @Override
        public ReadHandler getReadHandler() {
            return readHandler;
        }

        @Override
        public Writer getWriter() {
            return writer;
        }

        @Override
        public CloseListener getCloseListener() {
            return closeListener;
        }

        @Override
        public void close(CloseReason reason) {
            if (socket.isConnected()) {
                socket.close(reason.getCloseCode().getCode(), reason.getReasonPhrase());

                for (Extension extension : endpoint.getSupportedExtensions()) {
                    if (extension instanceof ExtendedExtension) {
                        try {
                            ((ExtendedExtension) extension).destroy(extensionContext);
                        } catch (Throwable t) {
                            // ignore.
                        }
                    }
                }
            }
        }
    }
}
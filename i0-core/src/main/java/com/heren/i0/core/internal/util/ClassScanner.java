package com.heren.i0.core.internal.util;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import org.glassfish.jersey.server.ResourceFinder;
import org.glassfish.jersey.server.internal.LocalizationMessages;
import org.glassfish.jersey.server.internal.scanning.FilesScanner;
import org.glassfish.jersey.server.internal.scanning.PackageNamesScanner;
import org.glassfish.jersey.server.internal.scanning.ResourceProcessor;
import org.objectweb.asm.*;

import java.io.IOException;
import java.io.InputStream;
import java.security.CodeSource;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.glassfish.jersey.internal.util.ReflectionHelper.classForNamePA;
import static org.glassfish.jersey.internal.util.ReflectionHelper.getContextClassLoaderPA;

public class ClassScanner {

    private static final Logger LOGGER =
            Logger.getLogger(ClassScanner.class.getName());

    private final ResourceFinder scanner;

    public ClassScanner(String... packages) {
        this.scanner = new PackageNamesScanner(packages, true);
    }

    public ClassScanner(CodeSource codeSource) {
        String path = codeSource.getLocation().getPath();
        this.scanner = path.endsWith(".jar") ? new FilesScanner(new String[]{path}, true) : new PackageNamesScanner(new String[]{""}, true);
    }

    public Set<Class<?>> findBy(Predicate<Class<?>> predicate) {
        PredicateScannerListener listener = new PredicateScannerListener(predicate);
        scanner.reset();
        while(scanner.hasNext()){
            final String next = scanner.next();
            if (listener.accept(next)) {
                final InputStream in = scanner.open();
                try {
                    listener.process(next, in);
                } catch (IOException e) {
                    LOGGER.log(Level.WARNING, LocalizationMessages.RESOURCE_CONFIG_UNABLE_TO_PROCESS(next));
                } finally {
                    try {
                        in.close();
                    } catch (IOException ex) {
                        LOGGER.log(Level.FINER, "Error closing resource stream.", ex);
                    }
                }
            }
        }
        return listener.found();
    }

    private class PredicateScannerListener implements ResourceProcessor, ClassVisitor {
        private final ClassLoader classloader = getContextClassLoaderPA().run();
        private final Predicate<Class<?>> predicate;
        private final ImmutableSet.Builder<Class<?>> found = new ImmutableSet.Builder<>();

        private PredicateScannerListener(Predicate<Class<?>> predicate) {
            this.predicate = predicate;
        }

        public Set<Class<?>> found() {
            return found.build();
        }

        @Override
        public boolean accept(String name) {
            return name.endsWith(".class");
        }

        @Override
        public void process(String name, InputStream in) throws IOException {
            new ClassReader(in).accept(this, 0);
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            try {
                Class aClass = checkNotNull(classForNamePA(name.replaceAll("/", "."), classloader).run());
                if (predicate.apply(aClass)) found.add(aClass);
            } catch (Throwable ignore) {
            }
        }

        @Override
        public void visitSource(String source, String debug) {
        }

        @Override
        public void visitOuterClass(String owner, String name, String desc) {
        }

        @Override
        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            return null;
        }

        @Override
        public void visitAttribute(Attribute attr) {
        }

        @Override
        public void visitInnerClass(String name, String outerName, String innerName, int access) {
        }

        @Override
        public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
            return null;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            return null;
        }

        @Override
        public void visitEnd() {
        }
    }
}

package com.heren.i0.gradle.core

import org.gradle.api.Named

class Environment implements Named {
    String name
    Hosting hosting

    Environment(String name) {
        this.name = name
    }
}

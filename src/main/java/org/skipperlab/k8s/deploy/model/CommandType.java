package org.skipperlab.k8s.deploy.model;

public enum CommandType {
    Create,
    Update,
    Delete,
    Pause,
    Resume,
    Restart,
    List,
    ConnectorPlugins,
    Get,
    Status
}

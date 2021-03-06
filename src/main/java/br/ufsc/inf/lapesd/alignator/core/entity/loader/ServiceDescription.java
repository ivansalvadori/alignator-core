package br.ufsc.inf.lapesd.alignator.core.entity.loader;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.UriBuilder;

public class ServiceDescription {

    private String ipAddress;
    private String serverPort;
    private String uriBase;
    private List<SemanticResource> semanticResources = new ArrayList<>();

    public String getMicroserviceFullPath() {
        UriBuilder builder = UriBuilder.fromPath("http://{ipAddress}:{serverPort}").path(uriBase);
        URI uri = builder.build(ipAddress, serverPort);
        return uri.toASCIIString();
    }

    public String getUriBase() {
        return uriBase;
    }

    public void setUriBase(String uriBase) {
        this.uriBase = uriBase;
    }

    public List<SemanticResource> getSemanticResources() {
        return semanticResources;
    }

    public void setSemanticResources(List<SemanticResource> semanticResources) {
        this.semanticResources = semanticResources;
    }

    public String getServerPort() {
        return serverPort;
    }

    public void setServerPort(String serverPort) {
        this.serverPort = serverPort;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
}
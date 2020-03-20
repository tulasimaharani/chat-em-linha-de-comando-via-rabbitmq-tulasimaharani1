package br.ufs.dcomp.ChatRabbitMQ;

import java.util.List;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import java.util.HashMap;
import java.util.Map;

public class Grupo {
    
    @SerializedName("source")
    @Expose
    private String source;
    
    @SerializedName("destination")
    @Expose
    private String destination;
    /*
    @SerializedName("vhost")
    @Expose
    private String vhost;

    @SerializedName("destination_type")
    @Expose
    private String destinationType;
    
    @SerializedName("routing_key")
    @Expose
    private String routingKey;
    
    @SerializedName("arguments")
    @Expose
    private List<Object> arguments = null;
    
    @SerializedName("properties_key")
    @Expose
    private String propertiesKey;
    */
    // Ignora todas as propriedades que não serão usadas
    private Map<String , Object> otherProperties = new HashMap<String , Object>();
    
    public String getSource() {
        return source;
    }
    
    public void setSource(String source) {
        this.source = source;
    }
    
    public String getDestination() {
        return destination;
    }
    
    public void setDestination(String destination) {
        this.destination = destination;
    }
    
    public Object get(String name) {
		return otherProperties.get(name);
	}
    /* 
    public String getVhost() {
        return vhost;
    }
    
    public void setVhost(String vhost) {
        this.vhost = vhost;
    }
    
    public String getDestinationType() {
        return destinationType;
    }
    
    public void setDestinationType(String destinationType) {
        this.destinationType = destinationType;
    }
    
    public String getRoutingKey() {
        return routingKey;
    }
    
    public void setRoutingKey(String routingKey) {
        this.routingKey = routingKey;
    }
    
    public List<Object> getArguments() {
        return arguments;
    }
    
    public void setArguments(List<Object> arguments) {
        this.arguments = arguments;
    }
    
    public String getPropertiesKey() {
        return propertiesKey;
    }
    
    public void setPropertiesKey(String propertiesKey) {
        this.propertiesKey = propertiesKey;
    }
    */
}
package br.ufs.dcomp.ChatRabbitMQ;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;

import com.google.gson.Gson; 
import com.google.gson.reflect.TypeToken;

public class RestClient {
    
    private String grupo;
    private String user;
    
    public RestClient(String grupo, String user){
        
        this.grupo = grupo;
        this.user = user;
        
    }
    
    public void run(){
        try {
            /*
            String username = "tulasi";//alterar
            String password = "try@g@1n";
            */
            String username = "ekqplyqf";
            String password = "qhMwhQEYGsdRMP0kWKcnbHHpiKy4g7sI";
     
            String usernameAndPassword = username + ":" + password;
            String authorizationHeaderName = "Authorization";
            String authorizationHeaderValue = "Basic " + java.util.Base64.getEncoder().encodeToString( usernameAndPassword.getBytes() );
     
            // Perform a request 
            // String restResource = "http://loadbalancer-a3d1b43088e5a03a.elb.us-east-1.amazonaws.com/";
            String restResource = "https://beaver.rmq.cloudamqp.com/";
            Client client = ClientBuilder.newClient();
            Response resposta;
            Gson gson = new Gson();
            if(grupo.isEmpty()){
                resposta = client.target( restResource )
                .path(user)// Lista todos os binds que a queue user possui
                .request(MediaType.APPLICATION_JSON)
                .header( authorizationHeaderName, authorizationHeaderValue ) // The basic authentication header goes here
                .get();     // Perform a post with the form values
                
                // Deserialização do Json em uma coleção de classes com Gson
                if (resposta.getStatus() == 200) {
                    String json = resposta.readEntity(String.class);
                    Type datasetListType = new TypeToken<Collection<Grupo>>() {}.getType();
            		List<Grupo> groups = gson.fromJson(json, datasetListType);
            		// Acessa cada classe individualmente e recupera o atributo desejado
            		for (Grupo g : groups) {
            		    String grupo = g.getSource();
            		    if (!grupo.isEmpty()){
            		        System.out.print(grupo + ", ");
            		    }
            		}
            		System.out.println();
                } else {
                    System.out.println(resposta.getStatus());
                } 
            }else{
                resposta = client.target( restResource )
                .path(grupo) // Lista todos os binds que tem o exchange grupo como source	
                .request(MediaType.APPLICATION_JSON)
                .header( authorizationHeaderName, authorizationHeaderValue ) // The basic authentication header goes here
                .get();     // Perform a post with the form values
                
                // Deserialização do Json em uma coleção de classes com Gson
                if (resposta.getStatus() == 200) {
                    String json = resposta.readEntity(String.class);
                    Type datasetListType = new TypeToken<Collection<Grupo>>() {}.getType();
            		List<Grupo> groups = gson.fromJson(json, datasetListType);
            		// Acessa cada classe individualmente e recupera o atributo desejado
            		for (Grupo g : groups) {
            		    String grupo = g.getDestination();
            			if (!grupo.isEmpty()){
            		        System.out.print(grupo + ", ");
            		    }
            		}
            	    System.out.println();
                } else {
                    System.out.println(resposta.getStatus());
                } 
                
            }
            
		} catch (Exception e) {
			e.printStackTrace();
		}
    }
}

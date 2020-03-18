package br.ufs.dcomp.ChatRabbitMQ;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import com.google.gson.Gson; 

public class RestClient {
    
    private String grupo;
    private String user;
    
    public RestClient(String grupo, String user){
        
        this.grupo = grupo;
        this.user = user;
        
    }
    
    public void run(){
        try {
            //gSON pode ser uma api usada (acrescentar nas dependencias do pom)
            //mavenrepository ascha as dependencia
            //olhar como usar gson
            //json pretty print 
            //acessa o elemento da lista onde ta o grupo(exchange) [{},{}]
            //api do rabbit http://loadbalancer-a3d1b43088e5a03a.elb.us-east-1.amazonaws.com/api/
            // JAVA 8 como pré-requisito (ver README.md)
            
            String username = "tulasi";//alterar
            String password = "try@g@1n";
     
            String usernameAndPassword = username + ":" + password;
            String authorizationHeaderName = "Authorization";
            String authorizationHeaderValue = "Basic " + java.util.Base64.getEncoder().encodeToString( usernameAndPassword.getBytes() );
     
            // Perform a request 
            String restResource = "http://loadbalancer-a3d1b43088e5a03a.elb.us-east-1.amazonaws.com/";//alteraqr usar balancer http
            Client client = ClientBuilder.newClient();
            Response resposta;
            if(grupo.isEmpty()){
                resposta = client.target( restResource )
                .path(user)// lista todos os binds que tem o exchange grupo como source
                .request(MediaType.APPLICATION_JSON)
                .header( authorizationHeaderName, authorizationHeaderValue ) // The basic authentication header goes here
                .get();     // Perform a post with the form values
            }else{
                resposta = client.target( restResource )
                .path(grupo) // lista todos os binds que a queue user possui	
                .request(MediaType.APPLICATION_JSON)
                .header( authorizationHeaderName, authorizationHeaderValue ) // The basic authentication header goes here
                .get();     // Perform a post with the form values
            }
            Gson gson = new Gson();
            
            
            if (resposta.getStatus() == 200) {
                Result result = gson.fromJson(resposta, Result.class);
                if (result != null){
                    for (Grupo g : result.getGrupos()){
                        System.out.println(g.getSource());
                    }
                }
            
            	//String json = resposta.readEntity(String.class);
                //System.out.println(json);
            } else {
                System.out.println(resposta.getStatus());
            }   
		} catch (Exception e) {
			e.printStackTrace();
		}
    }
}

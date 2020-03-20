package br.ufs.dcomp.ChatRabbitMQ;

import com.rabbitmq.client.*;
import java.nio.file.*;

public class EnviaArquivo extends Thread {
    
    private byte[] MESSAGE;
    private String QUEUE_NAME;
    private String QUEUE_NAME_ARCHIVE;
    private String EXCHANGE_NAME;
    private String PATH_ARCHIVE;
    private String EXCHANGE_NAME_ARCHIVE;
    
    public EnviaArquivo(byte[] MESSAGE, String QUEUE_NAME, String QUEUE_NAME_ARCHIVE, String EXCHANGE_NAME, String EXCHANGE_NAME_ARCHIVE, String PATH_ARCHIVE){
        
        this.MESSAGE = MESSAGE;
        this.QUEUE_NAME = QUEUE_NAME;
        this.QUEUE_NAME_ARCHIVE = QUEUE_NAME_ARCHIVE;
        this.EXCHANGE_NAME = EXCHANGE_NAME;
        this.EXCHANGE_NAME_ARCHIVE = EXCHANGE_NAME_ARCHIVE;
        this.PATH_ARCHIVE = PATH_ARCHIVE;

    }
    
    public void run(){
        try{
            /*
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost("LoadBalancer2-e12cd53c22f9d99f.elb.us-east-1.amazonaws.com"); // Alterar
            factory.setUsername("tulasi"); // Alterar
            factory.setPassword("try@g@1n"); // Alterar
            factory.setVirtualHost("/");
            Connection connection = factory.newConnection();
            Channel channel = connection.createChannel();
            */
            
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost("beaver.rmq.cloudamqp.com"); // Alterar
            factory.setUsername("ekqplyqf"); // Alterar
            factory.setPassword("qhMwhQEYGsdRMP0kWKcnbHHpiKy4g7sI"); // Alterar
            factory.setVirtualHost("ekqplyqf");
            Connection connection = factory.newConnection();
            Channel channel = connection.createChannel();
            
            if(EXCHANGE_NAME.isEmpty()){
                // Publica os arquivos enviados na queue do receptor
                                //  (exchange, routingKey, props, message-body); 
                channel.basicPublish("", QUEUE_NAME_ARCHIVE, null, MESSAGE);
                System.out.println("Arquivo \"" + PATH_ARCHIVE + "\" foi enviado para @" + QUEUE_NAME);
            }else{
                // Publica os arquivos enviados na exchange do grupo
                                //  (exchange, routingKey, props, message-body); 
                channel.basicPublish(EXCHANGE_NAME_ARCHIVE, "" , null, MESSAGE);
                System.out.println("Arquivo \"" + PATH_ARCHIVE + "\" foi enviado para #" + EXCHANGE_NAME);
            }
        } catch (Exception e){
            System.out.println("Não foi possivel enviar o arquivo!\n" +
                            "Confira se o caminho está correto!\n" +
                            "Ex: /home/ubuntu/environment/arquivo.tipo\n");
        }
    }
}
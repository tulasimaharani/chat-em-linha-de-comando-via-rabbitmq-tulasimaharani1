package br.ufs.dcomp.ChatRabbitMQ;

import com.rabbitmq.client.*;
import java.io.IOException;
import java.util.*;
import java.time.format.DateTimeFormatter;   
import java.time.LocalDateTime; 

public class Chat {

  //o usuário digita o seu nome de usuário. Exemplo: User: tarcisiorocha
  public static String createUser(){
    System.out.println("User: ");
    Scanner in = new Scanner(System.in);//canal de entrada de dados
    String user = in.nextLine();//le nome de usuario
    return user;
  }

  public static String getTime(){
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("(dd/MM/yyyy HH:mm) ");
    LocalDateTime date = LocalDateTime.now();  
    String data = formatter.format(date);
    return data;
  }

  public static void main(String[] argv) throws Exception {
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost("beaver.rmq.cloudamqp.com"); // Alterar
    factory.setUsername("ekqplyqf"); // Alterar
    factory.setPassword("qhMwhQEYGsdRMP0kWKcnbHHpiKy4g7sI"); // Alterar
    factory.setVirtualHost("ekqplyqf");
    Connection connection = factory.newConnection();
    Channel channel = connection.createChannel();
    
    String user = createUser();//define usuario
    
    String QUEUE_NAME = user;//"minha-fila" cria fila do usuario
                      //(queue-name, durable, exclusive, auto-delete, params); 
    channel.queueDeclare(QUEUE_NAME, false,   false,     false,       null);
    
    String prompt = ">>";
    Scanner in = new Scanner(System.in); // Canal de entrada de dados
    while(true){
      
      System.out.print(prompt);
      String message1 = in.nextLine();
      
      //identifica o user do receptor
      if (message1.charAt(0) == '@'){
        QUEUE_NAME = message1.substring(1, message1.length());
                          //(queue-name, durable, exclusive, auto-delete, params); 
        channel.queueDeclare(QUEUE_NAME, false,   false,     false,       null);
        prompt = message1 + ">>";
        System.out.print(prompt);
        message1 = in.nextLine();
      }
      
      //construção da mensagem
      String date = getTime();
      message1 = date + user + " diz: " + message1;
      //(21/09/2016 às 20:55) joaosantos diz: Opa!
      
      //publica as mensagens enviadas na queue do receptor
                     //  (exchange, routingKey, props, message-body             ); 
      channel.basicPublish("",       QUEUE_NAME, null,  message1.getBytes("UTF-8"));

      //Recebe as mensagens 
      Consumer consumer = new DefaultConsumer(channel) {
        public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)           throws IOException {
          String message = new String(body, "UTF-8");
          System.out.println(message);
        }
        
      };
      
      //(queue-name, autoAck, consumer);    
      channel.basicConsume(user, true,    consumer);
      
    }
    
  }
}
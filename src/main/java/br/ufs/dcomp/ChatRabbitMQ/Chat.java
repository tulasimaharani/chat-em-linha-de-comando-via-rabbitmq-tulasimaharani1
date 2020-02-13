package br.ufs.dcomp.ChatRabbitMQ;

import com.rabbitmq.client.*;
import java.io.IOException;
import java.util.*;
import java.time.format.DateTimeFormatter;   
import java.time.LocalDateTime; 
import com.google.protobuf.ByteString;


public class Chat {

  // O usuário digita o seu nome de usuário. Exemplo: User: tarcisiorocha
  public static String createUser(){
    System.out.println("User: ");
    Scanner in = new Scanner(System.in);// Canal de entrada de dados
    String user = in.nextLine();// Le nome de usuario
    return user;
  }

  // Captura hora local (do servidor)
  public static String[] getTime(){
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("(dd/MM/yyyy HH:mm) ");
    LocalDateTime date = LocalDateTime.now();  
    String data = formatter.format(date);
    String[] dataHora = data.split("\\s");
    return dataHora;
  }
  
  // Constroi proto de mensagem (marshalling)
  public static byte[] mensagemConstrutor(String input, String user, String grupo){
    String[] date = getTime();
    
    MensagemProto.Mensagem.Builder msg = MensagemProto.Mensagem.newBuilder();
    MensagemProto.Conteudo.Builder conteudo = MensagemProto.Conteudo.newBuilder();
    msg.setEmissor(user);
    msg.setData(date[0]);
    msg.setHora(date[1]);
    msg.setGrupo(grupo);
    conteudo.setTipo("text/plain");
    conteudo.setCorpo(ByteString.copyFromUtf8(input));
    conteudo.setNome("");
    msg.setConteudo(conteudo);
    
    MensagemProto.Mensagem pacote = msg.build();
    byte[] buffer = pacote.toByteArray();
    return buffer;
  }

  public static void main(String[] argv) throws Exception {
    // Cria conexão
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost("beaver.rmq.cloudamqp.com"); // Alterar
    factory.setUsername("ekqplyqf"); // Alterar
    factory.setPassword("qhMwhQEYGsdRMP0kWKcnbHHpiKy4g7sI"); // Alterar
    factory.setVirtualHost("ekqplyqf");
    Connection connection = factory.newConnection();
    Channel channel = connection.createChannel();
    
    // Define usuario
    String user = createUser();
    
    // Cria fila do usuario no RabbitMQ
    String QUEUE_NAME = user;
                     // (queue-name, durable, exclusive, auto-delete, params); 
    channel.queueDeclare(QUEUE_NAME, false,   false,     false,       null);
    String EXCHANGE_NAME = ".";
    
    // Captura e envio de mensagens 
    String prompt = ">>";
    Scanner in = new Scanner(System.in); 
    while(true){
      // Recebe as mensagens 
      Consumer consumer = new DefaultConsumer(channel) {
        public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)           throws IOException {
          try {
            MensagemProto.Mensagem message = MensagemProto.Mensagem.parseFrom(body);
            
            String emissor = message.getEmissor();
            String data = message.getData();
            String hora = message.getHora();
            String grupo = message.getGrupo();
            String corpo = message.getConteudo().getCorpo().toStringUtf8(); 
                   
            if(grupo.isEmpty()){
              System.out.println(data + " às " + hora + " " + emissor + " diz: " + corpo);
            }else{
              System.out.println(data + " às " + hora + " " + emissor + "#" + grupo + " diz: " + corpo);
            }
              
            } catch(IOException e) {
              System.out.println(e.getMessage());
            }
          }
        };
      // Publica as mensagens recebidas na fila do usuario
                   //(queue-name, autoAck, consumer);    
      channel.basicConsume(user, true,    consumer);


      System.out.print(prompt);
      String input = in.nextLine();
      
      // Identifica o objetivo da mensagem
      char simbolo = input.charAt(0);
      switch (simbolo) {
        // Chaveia para um usuario 
        case '@': {
            // Cria fila do usuario
            // Operações com queue inexistente é proibido, então 
            // caso o usuario ja exista, é ignorado 
            QUEUE_NAME = input.substring(1, input.length());
                              //(queue-name, durable, exclusive, auto-delete, params); 
            channel.queueDeclare(QUEUE_NAME, false,   false,     false,       null);
          
            prompt = input + ">>";
            
          break;
        }
        // Chaveia para um grupo 
        case '#': {
            // Cria exchange do grupo
            // Operações com exchange inexistente é proibido, então 
            // caso o grupo ja exista, é ignorado 
            EXCHANGE_NAME = input.substring(1, input.length());
            channel.exchangeDeclare(EXCHANGE_NAME, "fanout");

            prompt = input + ">>";

          break;
        }
        // Comandos
        case '!': {
          String[] comando = input.split("\\s");
          if(comando.length < 2){
            System.out.println("Comando invalido!\n" + 
              "Exemplo de comandos:\n" +
              " !addGroup amigos\n" +
              " !addUser joaosantos amigos\n" +
              " !delFromGroup joaosantos amigos\n" +
              " !removeGroup amigos\n");
              break;
          }
          switch (comando[0]) {
            // Cria um novo grupo
            // !addGroup amigos
            case "!addGroup": {
              // Nome do grupo
              EXCHANGE_NAME = comando[1];
                                //(exchange-name, tipe); 
              // Cria exchange do grupo
              channel.exchangeDeclare(EXCHANGE_NAME, "fanout");
              
              // Relaciona(binding) o criador ao grupo
              channel.queueBind(user, EXCHANGE_NAME, "");
              
              break;
            }
            // Adciona um usuario a um grupo
            // !addUser joaosantos amigos
            case "!addUser": {
              if(comando.length < 3){
                System.out.println("Comando invalido!\n" + 
                  "Exemplo de comandos:\n" +
                  " !addGroup amigos\n" +
                  " !addUser joaosantos amigos\n" +
                  " !delFromGroup joaosantos amigos\n" +
                  " !removeGroup amigos\n");
                break; 
              }

              // Nome do usuario            
              QUEUE_NAME = comando[1];
              // Nome do grupo             
              EXCHANGE_NAME = comando[2];
              
              // Cria a queue do usuario
                                //(queue-name, durable, exclusive, auto-delete, params); 
              channel.queueDeclare(QUEUE_NAME, false,   false,     false,       null);
              
              // Cria exchange do grupo
              // Operações com exchange inexistente é proibido, então 
              // caso o grupo ja exista é ignorado 
              channel.exchangeDeclare(EXCHANGE_NAME, "fanout");
              
              // Relaciona(binding) a queue do usuario ao exchange 
              channel.queueBind(QUEUE_NAME, EXCHANGE_NAME, "");
              
              break;
            }
            // Remove um usuario de um grupo
            // !delFromGroup joaosantos amigos
            case "!delFromGroup": {
              if(comando.length < 3){
                System.out.println("Comando invalido!\n" + 
                  "Exemplo de comandos:\n" +
                  " !addGroup amigos\n" +
                  " !addUser joaosantos amigos\n" +
                  " !delFromGroup joaosantos amigos\n" +
                  " !removeGroup amigos\n");
                break;
              }
              
              QUEUE_NAME = comando[1];
              EXCHANGE_NAME = comando[2];
              channel.queueUnbind(QUEUE_NAME, EXCHANGE_NAME, "");
              
              break;
            }
            // Remove um grupo e consequentemente seus usuarios
            // !removeGroup amigos
            case "!removeGroup": {
              EXCHANGE_NAME = comando[1];
              channel.exchangeDelete(EXCHANGE_NAME);
              break;
            }
            default: {
              System.out.println("Comando invalido!\n" + 
              "Exemplo de comandos:\n" +
              " !addGroup amigos\n" +
              " !addUser joaosantos amigos\n" +
              " !delFromGroup joaosantos amigos\n" +
              " !removeGroup amigos\n");
              break;
            }
          }
          break;
        }
        default: {
          
          // Envia mensagem a um usuario
          if(prompt.charAt(0) == '@'){
            
            // Metodo construtor de mensagens
            byte[] send = mensagemConstrutor(input, user, "");
            
            // Publica as mensagens enviadas na queue do receptor
                            //  (exchange, routingKey, props, message-body); 
            channel.basicPublish("", QUEUE_NAME, null, send);
          }
          
          // Envia mensagem a um grupo
          if(prompt.charAt(0) == '#'){

            // Metodo construtor de mensagens
            byte[] send = mensagemConstrutor(input, user, EXCHANGE_NAME);
            
            // Publica as mensagens enviadas na exchange do grupo
                            //  (exchange, routingKey, props, message-body); 
            channel.basicPublish(EXCHANGE_NAME, "" , null, send);
          }
          
          break;
        }
      }
    }
  }
}
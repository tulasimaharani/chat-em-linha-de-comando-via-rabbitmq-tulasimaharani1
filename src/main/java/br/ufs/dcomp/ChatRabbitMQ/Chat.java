package br.ufs.dcomp.ChatRabbitMQ;

import com.rabbitmq.client.*;
import java.io.*;
import java.util.*;
import java.time.format.DateTimeFormatter;   
import java.time.LocalDateTime; 
import com.google.protobuf.ByteString;
import java.nio.file.*;


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
  public static byte[] mensagemConstrutor(String input, String user, String grupo, String tipo_mime, String nome_conteudo, byte[] archive){
    String[] date = getTime();
    
    MensagemProto.Mensagem.Builder msg = MensagemProto.Mensagem.newBuilder();
    MensagemProto.Conteudo.Builder conteudo = MensagemProto.Conteudo.newBuilder();
    msg.setEmissor(user);
    msg.setData(date[0]);
    msg.setHora(date[1]);
    msg.setGrupo(grupo);
    
    conteudo.setTipo(tipo_mime);
    conteudo.setNome(nome_conteudo);
    if(tipo_mime.isEmpty()){
      conteudo.setCorpo(ByteString.copyFromUtf8(input));
    }else{
      conteudo.setCorpo(ByteString.copyFrom(archive));
    }
    msg.setConteudo(conteudo);
    
    MensagemProto.Mensagem pacote = msg.build();
    byte[] buffer = pacote.toByteArray();
    return buffer;
  }
  
  public static void printComandoInvalido(){
    System.out.println("Comando invalido!\n" + 
                "Exemplo de comandos:\n" +
                " !addGroup amigos\n" +
                " !addUser joaosantos amigos\n" +
                " !delFromGroup joaosantos amigos\n" +
                " !removeGroup amigos\n" +
                " !upload /home/tarcisio/aula1.pdf\n" +
                " !listUsers amigos\n" +
                " !listGroups\n");
  }

  public static void main(String[] argv) throws Exception {
    // Cria conexão
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost("LoadBalancer2-e12cd53c22f9d99f.elb.us-east-1.amazonaws.com"); // Alterar
    factory.setUsername("tulasi"); // Alterar
    factory.setPassword("try@g@1n"); // Alterar
    factory.setVirtualHost("/");
    Connection connection = factory.newConnection();
    Channel channel = connection.createChannel();
    
    // Define usuario
    String user = createUser();
    String userArchive = user + "Arquivo";
    
    // Cria fila do usuario no RabbitMQ para mensagens e para arquivos
    String QUEUE_NAME = user;
                     // (queue-name, durable, exclusive, auto-delete, params); 
    channel.queueDeclare(QUEUE_NAME, false,   false,     false,       null);
    
    String QUEUE_NAME_ARCHIVE = userArchive;
    channel.queueDeclare(QUEUE_NAME_ARCHIVE, false,   false,     false,       null);
    
    // Inicialização de variaveis
    String EXCHANGE_NAME = ".";
    String EXCHANGE_NAME_ARCHIVE = ".";
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
            String nome = message.getConteudo().getNome();
            String tipo = message.getConteudo().getTipo();
            
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
      
      // Recebe os arquivos
      Consumer consumerArchive = new DefaultConsumer(channel) {
        public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)           throws IOException {
          try {
            MensagemProto.Mensagem message = MensagemProto.Mensagem.parseFrom(body);
            
            String emissor = message.getEmissor();
            String data = message.getData();
            String hora = message.getHora();
            String grupo = message.getGrupo();
            byte[] corpo = message.getConteudo().getCorpo().toByteArray();
            String nome = message.getConteudo().getNome();
            String tipo = message.getConteudo().getTipo();
          
            File dir = new File("chat/downloads/");
            dir.mkdirs();
            
            File file = new File(dir, nome);
            FileOutputStream fos = new FileOutputStream(file);
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            bos.write(corpo);
            bos.flush();
            bos.close();
              
            if(grupo.isEmpty()){
              System.out.println(data + " às " + hora + " Arquivo \"" + nome + "\" recebido de @" + emissor);
            }else{
              System.out.println(data + " às " + hora + " Arquivo \"" + nome + "\" recebido de @" + emissor + "#" + grupo);
            }
          } catch(IOException e) {
            System.out.println(e.getMessage());
          }
        }
      };
        
      // Publica os arquivos recebidos na fila de arquivos do usuario
                       //(queue-name, autoAck, consumer);  
      channel.basicConsume(userArchive, true,    consumerArchive);

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
            QUEUE_NAME_ARCHIVE = QUEUE_NAME + "Arquivo";
                              //(queue-name, durable, exclusive, auto-delete, params); 
            channel.queueDeclare(QUEUE_NAME, false,   false,     false,       null);
                        //(queue-name, durable, exclusive, auto-delete, params); 
            channel.queueDeclare(QUEUE_NAME_ARCHIVE, false,   false,     false,       null);
          
            prompt = input + ">>";
            
          break;
        }
        // Chaveia para um grupo 
        case '#': {
            // Cria exchange do grupo
            // Operações com exchange inexistente é proibido, então 
            // caso o grupo ja exista, é ignorado 
            EXCHANGE_NAME = input.substring(1, input.length());
            EXCHANGE_NAME_ARCHIVE = EXCHANGE_NAME + "Arquivo";
            channel.exchangeDeclare(EXCHANGE_NAME, "fanout");
            channel.exchangeDeclare(EXCHANGE_NAME_ARCHIVE, "fanout");
            
            prompt = input + ">>";

          break;
        }
        // Comandos
        case '!': {
          String[] comando = input.split("\\s");
          if(comando.length < 2 && !comando[0].equals("!listGroups")){
            printComandoInvalido();
            break; 
          }
          switch (comando[0]) {
            // Cria um novo grupo
            // !addGroup amigos
            case "!addGroup": {
              // Nome do grupo
              EXCHANGE_NAME = comando[1];
              EXCHANGE_NAME_ARCHIVE = EXCHANGE_NAME + "Arquivo";                   
              // Cria exchange do grupo
                                    //(exchange-name, tipe);
              channel.exchangeDeclare(EXCHANGE_NAME, "fanout");
              channel.exchangeDeclare(EXCHANGE_NAME_ARCHIVE, "fanout");
              
              // Relaciona(binding) o criador ao grupo
              channel.queueBind(user, EXCHANGE_NAME, "");
              channel.queueBind(userArchive, EXCHANGE_NAME_ARCHIVE, "");
              
              break;
            }
            // Adciona um usuario a um grupo
            // !addUser joaosantos amigos
            case "!addUser": {
              if(comando.length < 3){
                printComandoInvalido();
                break; 
              }

              // Nome do usuario            
              QUEUE_NAME = comando[1];
              QUEUE_NAME_ARCHIVE = QUEUE_NAME + "Arquivo";
              // Nome do grupo             
              EXCHANGE_NAME = comando[2];
              EXCHANGE_NAME_ARCHIVE = EXCHANGE_NAME + "Arquivo";
              // Cria a queue do usuario
                                //(queue-name, durable, exclusive, auto-delete, params); 
              channel.queueDeclare(QUEUE_NAME, false,   false,     false,       null);
              channel.queueDeclare(QUEUE_NAME_ARCHIVE, false,   false,     false,       null);
              
              // Cria exchange do grupo
              // Operações com exchange inexistente é proibido, então 
              // caso o grupo ja exista é ignorado 
              channel.exchangeDeclare(EXCHANGE_NAME, "fanout");
              channel.exchangeDeclare(EXCHANGE_NAME_ARCHIVE, "fanout");
              
              // Relaciona(binding) a queue do usuario ao exchange 
              channel.queueBind(QUEUE_NAME, EXCHANGE_NAME, "");
              channel.queueBind(QUEUE_NAME_ARCHIVE, EXCHANGE_NAME_ARCHIVE, "");
              break;
            }
            // Remove um usuario de um grupo
            // !delFromGroup joaosantos amigos
            case "!delFromGroup": {
              if(comando.length < 3){
                printComandoInvalido();
                break;
              }
              
              QUEUE_NAME = comando[1];
              QUEUE_NAME_ARCHIVE = QUEUE_NAME + "Arquivo";
              EXCHANGE_NAME = comando[2];
              EXCHANGE_NAME_ARCHIVE = EXCHANGE_NAME + "Arquivo";
              channel.queueUnbind(QUEUE_NAME, EXCHANGE_NAME, "");
              channel.queueUnbind(QUEUE_NAME_ARCHIVE, EXCHANGE_NAME_ARCHIVE, "");
              
              break;
            }
            // Remove um grupo e consequentemente seus usuarios
            // !removeGroup amigos
            case "!removeGroup": {
              EXCHANGE_NAME = comando[1];
              EXCHANGE_NAME_ARCHIVE = EXCHANGE_NAME + "Arquivo";
              channel.exchangeDelete(EXCHANGE_NAME);
              channel.exchangeDelete(EXCHANGE_NAME_ARCHIVE);
              break;
            }
            // Envia um arquivo a um grupo ou a um usuario
            // !upload /home/tarcisio/aula1.pdf
            case "!upload": {
           
              String CAMINHO_ARQUIVO = comando[1];
              File file = new File(CAMINHO_ARQUIVO);
              Path source = Paths.get(CAMINHO_ARQUIVO);
              String tipo_mime = Files.probeContentType(source); 
              byte[] bytesArquivo = Files.readAllBytes(source);
            
              String[] split_caminho = CAMINHO_ARQUIVO.split("/");
              String nome_conteudo = split_caminho[split_caminho.length-1];
              
              // Envia o arquivo a um usuario
              if(prompt.charAt(0) == '@'){
                
                byte[] send = mensagemConstrutor(input, user, "", tipo_mime, nome_conteudo, bytesArquivo);
                EnviaArquivo enviar = new EnviaArquivo(send, QUEUE_NAME, QUEUE_NAME_ARCHIVE, "", "", CAMINHO_ARQUIVO);
                enviar.start();
               
                System.out.println("Enviando \""+ CAMINHO_ARQUIVO + "\" para @"+ QUEUE_NAME);
              }
              // Envia o arquivo a um grupo
              if(prompt.charAt(0) == '#'){
                
                byte[] send = mensagemConstrutor(input, user, EXCHANGE_NAME, tipo_mime, nome_conteudo, bytesArquivo);
                EnviaArquivo enviar = new EnviaArquivo(send, QUEUE_NAME, QUEUE_NAME_ARCHIVE, EXCHANGE_NAME, EXCHANGE_NAME_ARCHIVE, CAMINHO_ARQUIVO);
                enviar.start();
               
                System.out.println("Enviando \""+ CAMINHO_ARQUIVO + "\" para #"+ EXCHANGE_NAME);
              }
              break;
            }
            // Lista todos os usuários em um dado grupo 
            // !listUsers ufs
            case "!listUsers": {
              String grupo = comando[1];
              String currentUser = "";
              grupo = "/api/exchanges/%2f/" + grupo + "/bindings/source"; // lista todos os binds que tem o exchange grupo como source
              RestClient client = new RestClient(grupo, currentUser);
              client.run();
            	break;
            }   
            // Listar todos os grupos
            // !listGroups
            case "!listGroups": {
              String grupo = "";
              String currentUser = "/api/queues/%2f/" + user + "/bindings"; // lista todos os binds que a queue user possui	
              RestClient client = new RestClient(grupo, currentUser);
              client.run();
              break;
            }
            
            default: {
              printComandoInvalido();
              break; 
            }
          }
          break;
        }
        default: {
          byte[] bytesArquivo = null;
          String tipo_mime = "";
          String nome_conteudo = "";
          
          // Envia mensagem a um usuario
          if(prompt.charAt(0) == '@'){
            
            // Metodo construtor de mensagens
            byte[] send = mensagemConstrutor(input, user, "", tipo_mime, nome_conteudo, bytesArquivo);
            
            // Publica as mensagens enviadas na queue do receptor
                            //  (exchange, routingKey, props, message-body); 
            channel.basicPublish("", QUEUE_NAME, null, send);
          }
          
          // Envia mensagem a um grupo
          if(prompt.charAt(0) == '#'){

            // Metodo construtor de mensagens
            byte[] send = mensagemConstrutor(input, user, EXCHANGE_NAME, tipo_mime, nome_conteudo, bytesArquivo);
            
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
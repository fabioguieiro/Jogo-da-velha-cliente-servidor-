package jogo_da_velha;

import java.awt.BorderLayout;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Formatter;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

public class JDVServidor extends JFrame {

    private String[] board = new String[9]; //Tabuleiro do jogo
    private JTextArea outputArea; //gera saídas das jogadas
    private Player[] players;  //array de jogadores 
    private ServerSocket server; //variável do socket para conexao com os clientes 
    private int currentPlayer; //jogador atual 
    private final static int PLAYER_X = 0; //jogador com marca X
    private final static int PLAYER_O = 1; //jogador com marca O
    private final static String[] MARKS = {"X", "O"};  //vetor de marcas para as jogadas
    private ExecutorService runGame; //executa os jogadores 
    private Lock gameLock; //bloqueia a sincornizaçao do jogo
    private Condition otherPlayerConnected; //para esperar o outro jogador 
    private Condition otherPlayerTurn; //para esperar a jogada do outro jogador
    private String apelido;

    public JDVServidor() {     
        super("Jogo da Velha - Servidor"); //string exibida no topo da janela do servidor 

        runGame = Executors.newFixedThreadPool(2);//cria ExecutorService com uma thread para cada cliente 
        gameLock = new ReentrantLock();//cria um bloqueio para o jogo 

        otherPlayerConnected = gameLock.newCondition(); //condição dos dois jogadores estarem conectados 

        otherPlayerTurn = gameLock.newCondition(); //condição para esperar a vez do outro jogador 

        for (int i = 0; i < 9; i++) { //cria o tabuleiro
            board[i] = new String("");
        }
        players = new Player[2]; 
        currentPlayer = PLAYER_X;  //configura X como o primeiro a jogar 

        try {
            server = new ServerSocket(1996, 2); //configura a servidor na porta 1996
            
        } catch (IOException ioException) {
            ioException.printStackTrace();
            System.exit(1);
        }

        outputArea = new JTextArea(); //cria JTextArea para a saída 
        add(outputArea, BorderLayout.CENTER);
        outputArea.setText("Esperando Jogadores\n");
        setSize(300, 300); //tamanho da janela 
        setVisible(true); //configura visibilidade como verdadeiro 
            try {
            displayMessage("servidor: "+InetAddress.getLocalHost()+"\n");
        } catch (UnknownHostException ex) {
            Logger.getLogger(JDVServidor.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void execute() {

        for (int i = 0; i < players.length; i++) {
            try { 
                players[i] = new Player(server.accept(), i);  //cria players 
                runGame.execute(players[i]); //executa o RunGame passando players como parametro
            } catch (IOException ioException) {
                ioException.printStackTrace();
                System.exit(1);
            }
        }

        gameLock.lock(); //bloqueia o jogo

        try {
            players[PLAYER_X].setSuspended(false); //retoma o jogador X
            otherPlayerConnected.signal(); // acorda a tread do jogador X
        } finally { 
            gameLock.unlock(); //desbloqueia o jogo pra X
        }
    }

    private void displayMessage(final String messageToDisplay) { //exibe as mensagens do servidor 

        SwingUtilities.invokeLater( //exibe uma mensagem a partir da thread de despacho de eventos de execução  
                new Runnable() {
            public void run() {
                outputArea.append(messageToDisplay); //add msg 
            }
        }
        );
    }

    public boolean validateAndMove(int location, int player) {

        while (player != currentPlayer) { //enquanto este nao for o jogador atual, espere
            gameLock.lock();
            try {
                otherPlayerTurn.await(); //aguarda jogada do outro jogador 
            } catch (InterruptedException exception) {
                exception.printStackTrace();
            } finally {
                if(!isGameOver()){ //se o jogo está em estado de estar terminado
                gameLock.unlock(); //desbloqueia o jogo
                }
            }
        }
             isGameOver(); //chamada da função que verifica se o jogo está em estado de estar terminado 
        if (!isOccupied(location)) { 
            board[location] = MARKS[currentPlayer]; //marca a jogada no tabuleiro
            currentPlayer = (currentPlayer + 1) % 2; //troca jogador  
         
            players[currentPlayer].otherPlayerMoved(location); //mostra ao jogador atual que a ultima jogada aconteceu 
            
            gameLock.lock();//bloqueia o jogo para a thread atual 
            
            try {
                otherPlayerTurn.signal(); //sinaliza que o outro jogador  continue 
            } finally {
                gameLock.unlock(); //desbloqueia o jogo depois de sinalizar 
            }

            return true; // se a jogada foi valida 
        } else {
            return false; // se a jogada nao foi valida 
        }
    }

    public boolean isOccupied(int location) { // função verifica se o quadro escolhido está disponivel ou nao 
        if (board[location].equals(MARKS[PLAYER_X])
                || board[location].equals(MARKS[PLAYER_O])) {
            return true; //retorno positivo 
        } else {
            return false; //retorno negativo 
        }
    }

    public boolean isGameOver() //função que descobre se o jogo foi terminado ou nao 
    {
        for(int i=0;i<MARKS.length;i++){
            if(board[0].equals(MARKS[i])&&board[1].equals(MARKS[i])&&board[2].equals(MARKS[i])){
                System.out.println(MARKS[i]+" win");
                players[i].enviarPacote(" voce ganhou!");
                players[(i+1)%2].enviarPacote(" voce perdeu :C!");
                return true;
            }else if(board[3].equals(MARKS[i])&&board[4].equals(MARKS[i])&&board[5].equals(MARKS[i])){
           System.out.println(MARKS[i]+" win");
                players[i].enviarPacote(" voce ganhou!");
                players[(i+1)%2].enviarPacote(" voce perdeu :C!");
                return true;
            }else if(board[6].equals(MARKS[i])&&board[7].equals(MARKS[i])&&board[8].equals(MARKS[i])){
           System.out.println(MARKS[i]+" win");
                players[i].enviarPacote(" voce ganhou!");
                players[(i+1)%2].enviarPacote(" voce perdeu :C!");
                return true;
            }else if(board[0].equals(MARKS[i])&&board[3].equals(MARKS[i])&&board[6].equals(MARKS[i])){
           System.out.println(MARKS[i]+" win");
                players[i].enviarPacote(" voce ganhou!");
                players[(i+1)%2].enviarPacote(" voce perdeu :C!");
                return true;
            }else if(board[1].equals(MARKS[i])&&board[4].equals(MARKS[i])&&board[7].equals(MARKS[i])){
           System.out.println(MARKS[i]+" win");
                players[i].enviarPacote(" voce ganhou!");
                players[(i+1)%2].enviarPacote(" voce perdeu :C!");
                return true;
            }else if(board[2].equals(MARKS[i])&&board[5].equals(MARKS[i])&&board[8].equals(MARKS[i])){
           System.out.println(MARKS[i]+" win");
                players[i].enviarPacote(" voce ganhou!");
                players[(i+1)%2].enviarPacote(" voce perdeu :C!");
                return true;
            }else if(board[0].equals(MARKS[i])&&board[4].equals(MARKS[i])&&board[8].equals(MARKS[i])){
           System.out.println(MARKS[i]+" win");
                players[i].enviarPacote(" voce ganhou!");
                players[(i+1)%2].enviarPacote(" voce perdeu :C!");
                return true;
            }else if(board[2].equals(MARKS[i])&&board[4].equals(MARKS[i])&&board[6].equals(MARKS[i])){
           System.out.println(MARKS[i]+" win");
                players[i].enviarPacote(" voce ganhou!");
                players[(i+1)%2].enviarPacote(" voce perdeu :C!");
                return true;
            }
            
        
        }
        int totalDeLugaresVagos=0; //variavel para contar quantidade de espaços vagos
        for(int j=0;j<9;j++){
            if(!board[j].equals(MARKS[0])&&!board[j].equals(MARKS[1])){
                totalDeLugaresVagos++;
            }
        }
        if(totalDeLugaresVagos==0){ //se nao existe espaços vagos, e nenhum joador ganhou, quer dizer que o jogo ficou empatado
         System.out.println("EMPATE");
                players[PLAYER_X].enviarPacote(" empatou :l");
                players[PLAYER_O].enviarPacote(" empatou :l");
                return true;
        }
        return false;
    }

    private class Player implements Runnable { // classe interna privada que gerencia cada player como um executavel 

        private Socket connection; //conexao com o cliente 
        private Scanner input; //entrada do cliente 
        private Formatter output; //saída para o cliente 
        private int playerNumber; //numero do jogador 
        private String mark; //marca associada a ele 
        private boolean suspended = true; //se está suspenso ou nao 
        private String apelido; //apelido para este jogador 

        public Player(Socket socket, int number) { //configurador da thread player 
            playerNumber = number; //set do numero do jogador
            mark = MARKS[playerNumber]; // set da marca 
            connection = socket; //set do socket 

            try { //conecta
                input = new Scanner(connection.getInputStream());
                output = new Formatter(connection.getOutputStream());
            } catch (IOException ioException) {
                ioException.printStackTrace();
                System.exit(1);
            }
        }
        
        public void enviarPacote(String txt){//troca de mensagens entre cliente e sevidor 
        output.format("fim do jogo\n");
            output.flush();
         output.format(txt);
            output.flush();
        }

        public void otherPlayerMoved(int location) { //envia mensagem ao jogador anunciando a jogada do seu oponente
           
            output.format("Movimento do oponente\n");
            output.format("%d\n", location);
            output.flush();
            
        }

        public void run() { //execuçao da thread

            try {
                displayMessage("Jogador " + mark + " conectado\n");
                output.format("%s\n", mark); //envia a marca do joggador 
                output.flush();

                if (playerNumber == PLAYER_X) { //se for o X, aguardar o O 
                    output.format("%s\n%s", "Jogador X conectado",
                            "esperando por outro jogador\n");
                    output.flush();
                    gameLock.lock(); //bloqueia o jogo para esperar o adversario
                    try {
                        while (suspended) {
                            otherPlayerConnected.await();
                        }
                    } catch (InterruptedException exception) {
                        exception.printStackTrace();
                    } finally {
                        gameLock.unlock(); //apos a chegada do oponente desbloqueia o jogo 
                    }

                    output.format("Outro jogador conectou .sua vez .\n"); //se for o X, inicia o jogo 
                    output.flush();
                } else {//se for O, espera a jogada do X 
                    output.format("jogador O conectado, aguarde\n");
                    output.flush();
                }

                while (!isGameOver()) { //enquanto jogo nao encerrado 
                    int location = 0; //variavel de local da jogada
                    if (input.hasNext()) {
                        location = input.nextInt();//obtem a posiçao da jogada 
                    }

                    if (validateAndMove(location, playerNumber)) { //verifica uma jogada valida 
                        displayMessage("\nlocation: " + location); //exibe o local da jogada 
                        output.format("Movimento valido.\n"); 
                        output.flush();          
                    } else {
                        output.format("movimento invalido, tente novamente\n"); //se o movimento nao for valido exibe essa mensagem 
                        output.flush();
                    }
                }
            } finally {
                try {
                    connection.close(); //finaliza conexao com o cliente 
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                    System.exit(1);
                }
            }
        }

        public void setSuspended(boolean status) { //configura a suspensao ou nao da thread 
            suspended = status; //set do valor passado 
        }
            
        } 
}

package jogo_da_velha;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.LayoutManager;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.Socket;
import java.net.InetAddress;
import java.io.IOException;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.util.Formatter;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

public class JDVCliente extends JFrame implements Runnable {

    private JTextField idField; //campo onde é exibido a marca do jogador 
    private JTextField timerField; //campo para o timer 
    private JTextArea displayArea; //JText area para exibir a saída 
    private JPanel boardPanel; //painel para o tabuleiro
    private JPanel panel2; //painel para conter o tabuleiro 
    private Square[][] board; //tabuleiro do jogo 
    private Square currentSquare; //quadrado atuaç 
    private Socket connection; //conexao com o servidor 
    private Scanner input; //entrada a partir do servidor 
    private Formatter output; //saída para o servidor 
    private String ticTacToeHost; //nome do host para o servidor
    private String myMark; //marca do jogador
    private boolean myTurn; //variavel que guarda a informação da vez do jogador
    private final String X_MARK = "X"; //marca X
    private final String O_MARK = "O"; //marca Y
    private String apelido; //apelido do jogador 
    private Thread tempo; //contador de tempo da jogada 
    private boolean ligaDesliga=false;  //interruptor do cronometro 
    
    public JDVCliente(String host, String apelido) { //construtor seta as variaveis apelido e host
        this.apelido=apelido;
        ticTacToeHost = host;
    }

    public void inicializaTela(){ 
    displayArea = new JTextArea(4, 30);
        displayArea.setEditable(false);
        add(new JScrollPane(displayArea), BorderLayout.SOUTH);

        boardPanel = new JPanel(); //configura o páinel com os quadrados para o tabuleiro 
        boardPanel.setLayout(new GridLayout(3, 3, 0, 0)); 

        board = new Square[3][3]; //cria o tabuleiro 

        for (int row = 0; row < board.length; row++) { //loop pelas linhas
            for (int column = 0; column < board[row].length; column++) { //loop pelas colunas
                board[row][column] = new Square(" ", row * 3 + column);
                boardPanel.add(board[row][column]); //adiciona um quadrado 
            }
        }

        idField = new JTextField(); //configura o campo de texto 
        idField.setEditable(false);
        add(idField, BorderLayout.NORTH);
        
        timerField = new JTextField(); //configura o espaço do timer na janela 
        timerField.setEditable(false);
        timerField.setBounds(310, 70, 70, 70);
        add(timerField);
        
        panel2 = new JPanel(); //configura o painel para conter o boardpanel 
        panel2.add(boardPanel, BorderLayout.CENTER); //add o painel do tabuleiro 
        add(panel2, BorderLayout.CENTER); //add o peinel container 

        setSize(400, 325); //tamanho da janela 
        setVisible(true); //set da visibilidade da janela como verdadeiro 
    }
    
    public void startClient() { //inicia a thread do cliente 
        try { 

            connection = new Socket(
                    InetAddress.getByName(ticTacToeHost), 1996); //inicia conecção com o servidor na porta 1996 

            input = new Scanner(connection.getInputStream());//obtem o fluxo de entrada 
            output = new Formatter(connection.getOutputStream());//obtem o fuxo de saida 
            
            inicializaTela(); //chamada da função inicializa tela 
            
        } catch (IOException ioException) { //caso o cliente seja inicializado se um servidor ativo 
            ioException.printStackTrace();
            JOptionPane.showMessageDialog(rootPane,  "Algum erro ocorreu, verifique se o servidor esta online");
        }

        ExecutorService worker = Executors.newFixedThreadPool(1); //cria e executa a thread de trabalhador para este cliente 
        worker.execute(this);
    }

    public void run() { //cria e inicia thread de trabalhador para esse cliente 
            myMark = input.next(); //obtem a marca do jogador 

        SwingUtilities.invokeLater(
                new Runnable() {
            public void run() {
                idField.setText(apelido+" voce é o  \"" + myMark + "\"");
            }
        }
        );

        myTurn = (myMark.equals(X_MARK));//se a marca for X, myturn =1

        while (true) { //recebe as mensagens enviadas para o cliente e gera saídas delas
            if (input.hasNextLine()) {
                processMessage(input.nextLine());
            }
        }
    }

    private void processMessage(String message) {
        if(message.equals("fim do jogo")){ //se a mesagem é essa 
            String txt = input.nextLine(); //receba txt
               displayMessage(apelido+txt); //imprima o apelido do vencedor + txt
        }else if (message.equals("Movimento valido.")) { //se a mensagem é essa 
            displayMessage("Movimento valido, aguarde.\n"); //imprima isto 
            setMark(currentSquare, myMark); //marque no tabuleiro 
        } else if (message.equals("movimento invalido, tente novamente")) { //se a mensagem é esta 
            displayMessage(message + "\n"); //imprima a propria mensagem 
            myTurn = true; //a vez continua sendo deste jogador 
            iniciaThreadCronometro(); //inicia cronometro 
            
        }
        else if (message.equals("Movimento do oponente")) {
            int location = input.nextInt();//obtem a jogada 
            input.nextLine(); //pula uma linha depois da posição de location 
            int row = location / 3; //calcula a linha 
            int column = location % 3; //calcula a coluna 

            setMark(board[row][column],
                    (myMark.equals(X_MARK) ? O_MARK : X_MARK));//faz a marca com X ou O
            displayMessage("Movimento do oponente, sua vez . \n");
            myTurn = true;//passa a vez para este cliente
            iniciaThreadCronometro(); //inicia cronometro 
        } else {
            displayMessage(message + "\n");
        }
    }
    
    private void iniciaThreadCronometro(){ //contador de tempo de jogada 
   Cronometro x = new Cronometro();
    tempo = new Thread(x);
    tempo.start(); //inicia 
    ligaDesliga=true; //interruptor 
    }
    
    class Cronometro implements Runnable {
        public void run() {
              for (int i = 15; i > 0; i--){//por 15 vezes esperaremos 1 segundo 
				System.out.println(i + " segundos");
                                timerField.setText("    "+String.valueOf(i)+"    ");
                  try {
                      Thread.sleep(1000); // 1 segundo
                  } catch (InterruptedException ex) {
                      Logger.getLogger(JDVCliente.class.getName()).log(Level.SEVERE, null, ex);
                  }
                  
            if(!ligaDesliga){
            return;
            }
			}
              
            Random rn = new Random();
        int random = rn.nextInt(10);
                    setCurrentSquare(board[random/3][random%3]);
                    sendClickedSquare(random);
                   
        }

}

     private void displayTimer(final String messageToDisplay) { //função para exibição do cronometro 
        SwingUtilities.invokeLater(
                new Runnable() {
            public void run() {
                displayArea.append(messageToDisplay);
            }
        }
        );
    }
    
    private void displayMessage(final String messageToDisplay) { //manipula displayArea na threade de despacho de eventos
        SwingUtilities.invokeLater(
                new Runnable() {
            public void run() {
                displayArea.append(messageToDisplay);//atualiza a saída 
            }
        }
        );
    }

    private void setMark(final Square squareToMark, final String mark) { //manipula a marca que vai ser colocada sobre o tabuleiro 
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                squareToMark.setMark(mark);
            }
        }
        );
    }

    public void sendClickedSquare(int location) { //envia ao servidor qual quadrado foi clicado 
        if (myTurn) {
            output.format("%d\n", location);
            output.flush();
            myTurn = false;
        }
    }

    public void setCurrentSquare(Square square) { //configura o quadrado atual 
        currentSquare = square;
    }

    private class Square extends JPanel {//classe dos quadrados do tabuleiro 

        private String mark;
        private int location;

        public Square(String squareMark, int squarelocation) {
            mark = squareMark; //configura a marca para este quadrado 
            location = squarelocation;//configura a localizaao deste quadrado 

            addMouseListener(
                    new MouseAdapter() {
                public void mouseReleased(MouseEvent e) {
                   ligaDesliga=false;//interruptor do timer desliga 
                    setCurrentSquare(Square.this);  // configura o quadrado atual 
                    sendClickedSquare(getSquareLocation());//envia a posição deste quadrado 
                     
                }
            }
            );
        }
            
        public Dimension getPreferredSize() {//retorna o tamanho para o quadrado 
            return new Dimension(60, 60);
        }

        public Dimension getMinimumSize() {//retorna o tamanho minimo do quadrado 
            return getPreferredSize();
        }

        public void setMark(String newMark) { //configura a marca para o quadrado 
            mark = newMark; //set da marca 
            repaint(); //repinta o quadrado 
        }

        public int getSquareLocation() { //getter da localização 
            return location;
        }

        public void paintComponent(Graphics g) { //desenha o quadrado e no quadrado 
            super.paintComponent(g);

            g.drawRect(0, 0, 59, 59); //desenha o quadrado dado os parametros 
            g.drawString(mark, 28, 32); //desenha a marca no quadrado 
        }
    }
}

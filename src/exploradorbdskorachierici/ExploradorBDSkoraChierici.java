/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package exploradorbdskorachierici;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.DatabaseMetaData;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

/**
 *
 * @author lucass
 */
public class ExploradorBDSkoraChierici extends javax.swing.JFrame {

    static final String CAMINHO_ARQUIVO_CONEXAO = "dadosConexao.csv";
    private Connection con;
    private Statement stmt;
    private ResultSet rs;

    private boolean conectado = false;

    private boolean carregarDadosConexao() {

        try {
            boolean sucesso = true;
            FileReader arquivoConexao = new FileReader(CAMINHO_ARQUIVO_CONEXAO);
            BufferedReader bufferConexao = new BufferedReader(arquivoConexao);

            String linha = bufferConexao.readLine();
            if (linha == null) {
                sucesso = false;
            } else {
                String[] substrings = linha.split(";");
                if (substrings.length < 5) {
                    sucesso = false;
                } else {
                    SelecionadorSGBD.setSelectedItem(substrings[0]);
                    CampoURL.setText(substrings[1]);
                    CampoPorta.setText(substrings[2]);
                    CampoBD.setText(substrings[3]);
                    CampoUsuario.setText(substrings[4]);
                }
            }

            bufferConexao.close();
            arquivoConexao.close();
            return sucesso;
        } catch (FileNotFoundException e) {
            // não é um problema, simplesmente não havia dados salvos
        } catch (IOException ioe) {
            JOptionPane.showMessageDialog(null, "Erro lendo arquivo de conexão: " + ioe);
        }
        return false;
    }

    private boolean escreverDadosConexao() {
        try {
            FileWriter arquivoDados = new FileWriter(CAMINHO_ARQUIVO_CONEXAO);
            String SGBD = SelecionadorSGBD.getSelectedItem().toString();
            String URL = CampoURL.getText();
            String porta = CampoPorta.getText();
            String BD = CampoBD.getText();
            String usuario = CampoUsuario.getText();
            arquivoDados.write(SGBD + ";" + URL + ";" + porta + ";" + BD + ";" + usuario);
            arquivoDados.close();
        } catch (IOException ioe) {
            JOptionPane.showMessageDialog(null, "Erro escrevendo arquivo de conexão: " + ioe);
            return false;
        }
        return true;
    }

    private void fecharConexao() {
        try {
            conectado = false;
            if (rs != null && !rs.isClosed()) {
                rs.close();
            }
            if (stmt != null && !stmt.isClosed()) {
                stmt.close();
            }
            if (con != null && !con.isClosed()) {
                con.close();
            }
        } catch (SQLException sqle) {
        }
    }

    private void gerarArvore() {
        DefaultMutableTreeNode top = new DefaultMutableTreeNode(CampoBD.getText(), true);
        DefaultMutableTreeNode tabelas = new DefaultMutableTreeNode("tabelas", true);
        DefaultMutableTreeNode views = new DefaultMutableTreeNode("views", true);

        top.add(tabelas);
        top.add(views);

        try {
            DatabaseMetaData dbmd = con.getMetaData();
            String[] tipos = {"TABLE", "VIEW"};
            ResultSet tabelasRS = dbmd.getTables(CampoBD.getText(), null, null, tipos);
            while (tabelasRS.next()) {
                DefaultMutableTreeNode registro = new DefaultMutableTreeNode(tabelasRS.getString("TABLE_NAME"), false);
                String tipo = tabelasRS.getString("TABLE_TYPE");
                if (tipo.equals("TABLE")) {
                    tabelas.add(registro);
                } else if (tipo.equals("VIEW")) {
                    views.add(registro);
                }

            }
            tabelasRS.close();

        } catch (SQLException sqle) {
            JOptionPane.showMessageDialog(null, "Erro em banco de dados: " + sqle);
        }

        DefaultTreeModel modelo = new DefaultTreeModel(top);
        ArvoreDB.setModel(modelo);
    }
    
    private ArrayList<String> getPrimaryKeyColumns(String tableName) {
        ArrayList<String> pks = new ArrayList<>();
        try {
            DatabaseMetaData dbmd = con.getMetaData();
            ResultSet pkrs = dbmd.getPrimaryKeys(null, null, tableName);
            
            while(pkrs.next()) {
                pks.add(pkrs.getString("COLUMN_NAME"));
            }
            
        } catch(SQLException sqle) {
            JOptionPane.showMessageDialog(null, "Erro em banco de dados: " + sqle);
        }
        
        return pks;
    }
    
    private void gerarTabela() {
        DefaultTableModel table = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {                
                return false;               
            };
        };
        try {
           ResultSetMetaData rsmd = rs.getMetaData();
           DatabaseMetaData dbmd = con.getMetaData();
           int columnCount = rsmd.getColumnCount();
           for(int i = 1; i <= columnCount; ++i) {
                ArrayList<String> pks = getPrimaryKeyColumns(rsmd.getTableName(i));
                
                table.addColumn(rsmd.getColumnLabel(i) + "(" 
                    + rsmd.getColumnTypeName(i) 
                    + (pks.contains(rsmd.getColumnLabel(i)) ? " - PK" : "")
                    +")"
                );
           }
           while(rs.next()) {
                Object[] row = new Object[columnCount];
                for(int i = 0; i < columnCount; ++i) {
                    row[i] = rs.getObject(i + 1);
                }
                table.addRow(row);
           }
        } catch (SQLException sqle) {
            JOptionPane.showMessageDialog(null, "Erro em banco de dados: " + sqle);
        }
        
        TabelaRegistros.setModel(table);
    }
    
    private String preparaQuery(String query) {
        if(query.toLowerCase().contains("select") && (int)LimiteRegistros.getValue() > 0) {
            query = query.replace(";", "").concat(" limit " + LimiteRegistros.getValue() + ";");
            System.out.println(query);
        }
        
        return query;
    }

    /**
     * Creates new form ExploradorBDSkoraChierici
     */
    public ExploradorBDSkoraChierici() {
        initComponents();
        carregarDadosConexao();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane3 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        SelecionadorSGBD = new javax.swing.JComboBox<>();
        jLabel1 = new javax.swing.JLabel();
        CampoURL = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        CampoPorta = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        CampoBD = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        CampoUsuario = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        CampoSenha = new javax.swing.JPasswordField();
        jLabel6 = new javax.swing.JLabel();
        BotaoConectar = new javax.swing.JButton();
        jSeparator1 = new javax.swing.JSeparator();
        jScrollPane1 = new javax.swing.JScrollPane();
        ArvoreDB = new javax.swing.JTree();
        jScrollPane2 = new javax.swing.JScrollPane();
        AreaQuery = new javax.swing.JTextArea();
        filler1 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(32767, 0));
        jScrollPane4 = new javax.swing.JScrollPane();
        TabelaRegistros = new javax.swing.JTable();
        Executar = new javax.swing.JButton();
        jLabel7 = new javax.swing.JLabel();
        LimiteRegistros = new javax.swing.JSpinner();

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jScrollPane3.setViewportView(jTable1);

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosed(java.awt.event.WindowEvent evt) {
                formWindowClosed(evt);
            }
        });

        SelecionadorSGBD.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "MySQL/MariaDB", "postgreSQL" }));

        jLabel1.setText("SGBD");

        CampoURL.setColumns(15);

        jLabel2.setText("URL");

        CampoPorta.setColumns(5);

        jLabel3.setText("Porta");

        CampoBD.setColumns(15);

        jLabel4.setText("Banco de Dados");

        CampoUsuario.setColumns(10);

        jLabel5.setText("Usuário");

        CampoSenha.setColumns(10);

        jLabel6.setText("Senha");

        BotaoConectar.setText("Conectar");
        BotaoConectar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                BotaoConectarActionPerformed(evt);
            }
        });

        ArvoreDB.setModel(new DefaultTreeModel(new DefaultMutableTreeNode("BD")));
        ArvoreDB.addTreeSelectionListener(new javax.swing.event.TreeSelectionListener() {
            public void valueChanged(javax.swing.event.TreeSelectionEvent evt) {
                ArvoreDBValueChanged(evt);
            }
        });
        jScrollPane1.setViewportView(ArvoreDB);

        AreaQuery.setColumns(20);
        AreaQuery.setRows(5);
        jScrollPane2.setViewportView(AreaQuery);

        TabelaRegistros.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {},
                {},
                {},
                {}
            },
            new String [] {

            }
        ));
        jScrollPane4.setViewportView(TabelaRegistros);

        Executar.setText("Executar");
        Executar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ExecutarActionPerformed(evt);
            }
        });

        jLabel7.setText("Limite de registros:");

        LimiteRegistros.setToolTipText("Limite de registros para buscas. Use 0 para não limitar.");
        LimiteRegistros.setValue(1000);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(SelecionadorSGBD, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel1))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(CampoURL, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel2))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(CampoPorta, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(CampoBD, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabel3)
                                .addGap(46, 46, 46)
                                .addComponent(jLabel4)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(CampoUsuario, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel5))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel6)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(CampoSenha, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(BotaoConectar, javax.swing.GroupLayout.PREFERRED_SIZE, 86, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addContainerGap())
                    .addComponent(jSeparator1, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 182, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(Executar)
                                .addGap(18, 18, 18)
                                .addComponent(jLabel7)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(LimiteRegistros, javax.swing.GroupLayout.PREFERRED_SIZE, 87, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jScrollPane2)
                                    .addComponent(jScrollPane4))
                                .addGap(18, 18, 18)
                                .addComponent(filler1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(2, 2, 2))))))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(jLabel2)
                    .addComponent(jLabel3)
                    .addComponent(jLabel4)
                    .addComponent(jLabel5)
                    .addComponent(jLabel6))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(CampoPorta, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(SelecionadorSGBD, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(CampoURL, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(CampoUsuario, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(CampoSenha, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(CampoBD, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(BotaoConectar)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 4, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(Executar)
                            .addComponent(jLabel7)
                            .addComponent(LimiteRegistros, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jScrollPane2)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addGap(62, 62, 62)
                                .addComponent(filler1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(layout.createSequentialGroup()
                                .addGap(18, 18, 18)
                                .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))))
                    .addComponent(jScrollPane1))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void BotaoConectarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_BotaoConectarActionPerformed
        try {
            String driver;
            if (SelecionadorSGBD.getSelectedItem().equals("MySQL/MariaDB")) {
                driver = "mysql";
            } else if (SelecionadorSGBD.getSelectedItem().equals("postgreSQL")) {
                driver = "postgresql";
            } else {
                conectado = false;
                JOptionPane.showMessageDialog(null, "Driver selecionado indisponível: " + SelecionadorSGBD.getSelectedItem());
                return;
            }
            String URL = CampoURL.getText();
            String porta = CampoPorta.getText();
            String BD = CampoBD.getText();
            String usuario = CampoUsuario.getText();
            String senha = new String(CampoSenha.getPassword());
            fecharConexao();
            con = DriverManager.getConnection("jdbc:" + driver + "://" + URL + ":" + porta + "/" + BD, usuario, senha);
            stmt = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            conectado = true;
            escreverDadosConexao();
            JOptionPane.showMessageDialog(null, "Conexão ao banco de dados estabelecida com sucesso!");
            gerarArvore();
        } catch (SQLException sqle) {
            conectado = false;
            JOptionPane.showMessageDialog(null, "Erro em banco de dados: " + sqle);
        }
    }//GEN-LAST:event_BotaoConectarActionPerformed

    private void formWindowClosed(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosed
        fecharConexao();
    }//GEN-LAST:event_formWindowClosed

    private void ExecutarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ExecutarActionPerformed
        if(conectado) {
            try {
                String query = preparaQuery(AreaQuery.getText());
                rs = stmt.executeQuery(query);
                gerarTabela();
            } catch (SQLException sqle) {
                JOptionPane.showMessageDialog(null, "Erro na query: " + sqle);
            }
        } else {
            JOptionPane.showMessageDialog(null, "Conecte ao banco de dados antes de executar alguma query!");
        }
    }//GEN-LAST:event_ExecutarActionPerformed

    private void ArvoreDBValueChanged(javax.swing.event.TreeSelectionEvent evt) {//GEN-FIRST:event_ArvoreDBValueChanged
        Object[] path = evt.getPath().getPath();
        if(path.length == 3) {
            try {
                rs = stmt.executeQuery(preparaQuery("select * from " + path[2] + ";"));
            } catch (SQLException sqle) {
                JOptionPane.showMessageDialog(null, "Erro na query: " + sqle);
            }
            gerarTabela();
        }
    }//GEN-LAST:event_ArvoreDBValueChanged

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(ExploradorBDSkoraChierici.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(ExploradorBDSkoraChierici.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(ExploradorBDSkoraChierici.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(ExploradorBDSkoraChierici.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new ExploradorBDSkoraChierici().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextArea AreaQuery;
    private javax.swing.JTree ArvoreDB;
    private javax.swing.JButton BotaoConectar;
    private javax.swing.JTextField CampoBD;
    private javax.swing.JTextField CampoPorta;
    private javax.swing.JPasswordField CampoSenha;
    private javax.swing.JTextField CampoURL;
    private javax.swing.JTextField CampoUsuario;
    private javax.swing.JButton Executar;
    private javax.swing.JSpinner LimiteRegistros;
    private javax.swing.JComboBox<String> SelecionadorSGBD;
    private javax.swing.JTable TabelaRegistros;
    private javax.swing.Box.Filler filler1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JTable jTable1;
    // End of variables declaration//GEN-END:variables
}

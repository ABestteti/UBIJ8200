package br.com.acaosistemas.main;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;

import br.com.acaosistemas.db.connection.ConnectionFactory;
import br.com.acaosistemas.db.connection.DBConnectionInfo;
import br.com.acaosistemas.db.dao.UBIRuntimesDAO;
import br.com.acaosistemas.wsclientes.ClienteWSAssinarEvento;
import br.com.acaosistemas.wsclientes.ClienteWSConsultarLote;
import br.com.acaosistemas.wsclientes.ClienteWSCorreios;
import br.com.acaosistemas.wsclientes.ClienteWSEnviarLote;
import oracle.jdbc.OracleTypes;

public class Deamon {

	private static final int STOP_DAEMON            = 4;
	private static final int CONSULTAR_STATUS       = 5;
	
	private static final int DEAMON_ALIVE           = 1;

	private Connection conn;
	private CallableStatement stmt;
	
	public Deamon() {
		// TODO Auto-generated constructor stub
	}

	public static void main(String[] args) {

		Deamon procPoboxXml = new Deamon();
		
		String dbUserName = args[0];
		String dbPassWord = args[1];
		String dbStrConn  = args[2];

		
		// Salva em memoria as informacoes de conexao com o banco
		// de dados para posterior uso pela classes DAO.
		DBConnectionInfo.setDbUserName(dbUserName);
		DBConnectionInfo.setDbPassWord(dbPassWord);
		DBConnectionInfo.setDbStrConnect(dbStrConn);
		
		// Entra no loop de leitura da tabela UBI_POBOX_XML
		procPoboxXml.lerPipeDB();

	}
	
    private void lerPipeDB() {
		// Rowid de uma linha da table UBI_POBOX_XML
		String pipeConteudo  = "";
		
		// Variaveis para trabalhar com o pipe de banco
		String pipeName   = "";
		int    pipeCmd    = -1;
		int    pipeStatus = -1;
		
		// Controla o loop de leitura do PIPE
		boolean stopDeamon = false;
		
		// Objects de acesso as tabelas do banco de dados
		UBIRuntimesDAO runtimeDAO = new UBIRuntimesDAO();
		
		pipeName = runtimeDAO.getRuntimeValue("PIPEUBI");
		runtimeDAO.closeConnection();
		
		// Abre conexao com o banco para leitura do pipe do
		// banco de dados.
		conn = new ConnectionFactory().getConnection();
		
		System.out.println("Processando registros dos correios...");
		
		// Loop para leitura constante do pipe de comunicacao
		// do deamon e por procura de registros com status 0 (nao processado)
		// na tabela UBI_POBOX_XML
		while (!stopDeamon) {
			
			// Pausa a execucao da thread principal por 0.5 segundos
			// Com iso, e liberado o lock da dbms_pipe, permitindo que a 
			// consiliacao de usuario possa conceder grant da package para
			// o usuario que esta sem conciliado.
            try {
				Thread.sleep(500);
			} catch (InterruptedException e1) {
				throw new RuntimeException(e1);
			}
            
			// Prepara a chamada da funcao no banco de dados
			try {
				stmt = conn.prepareCall("{? = call dbms_pipe.receive_message(?,1)}");

				// Define que o tipo de retorno da funcao sera um NUMBER
				stmt.registerOutParameter(1, OracleTypes.NUMBER);

				// Define o nome do pipe que sera lido do banco.
				stmt.setString(2, pipeName);

				// Executa a funcao do banco
				stmt.execute();

				// Recupera o status da leitura do pipe do banco
				pipeStatus = stmt.getInt(1);

			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
			
			// Se o retorno do pipe foi obtido com sucesso (valor 0),
			// busca o comando enviado.
			if (pipeStatus == 0) {

				try {
					
					stmt = conn.prepareCall("BEGIN dbms_pipe.unpack_message(?); END;");

					// Define que o parametro e do tipo OUT, retornando um NUMBER
					// e um VARCHAR, respectivamente.
					stmt.registerOutParameter(1, OracleTypes.VARCHAR);

					// Executa a funcao do banco
					stmt.execute();

					// Recupera os valores retornados do pipe
					pipeConteudo = stmt.getString(1)
							;
				} catch (SQLException e) {
					throw new RuntimeException(e);
				}

				switch (pipeCmd) {
				case CONSULTAR_STATUS:
					System.out.println("Recebido comando status do servico!");
					
					// Nesse caso o objeto pipeConteudo armazena o nome do
					// pipe de retorno que sera usado para enviar o status
					// de volta para o PL/SQL, sinalizando que o daemon esta
					// rodando.
					statusDaemon(pipeConteudo);
			     	break;
				case STOP_DAEMON:
					System.out.println("Recebido comando stop do servico!");
					stopDeamon = true;
					break;
				}
			}
			
			try {
				if (!stmt.isClosed()) {
				   stmt.close();
				}
			} catch (SQLException e) {
				throw new RuntimeException(e) ;
			}
			
			// Inicia o processo de leitura dos registros n�o processados
			// na tabela UBI_POBOX_XML
			new ProcessarUbiPoboxXml().lerRegistrosNaoProcessados();
		}
		
		try {
			stmt.close();
	        conn.close();
		} catch (SQLException e) {
			throw new RuntimeException(e) ;
		}
		
		System.out.println("Servico encerrado por requisicao do usuario.");
	}
	
	private void statusDaemon(String pPipeReturn) {
		try {
			if (!stmt.isClosed()) {
				stmt.close();
			}
			
			// Retorna o status do deamon, informando
			// que ele esta ativo: DEAMON_ALIVE
			//dbms_pipe.pack_message(pipe_name);
			stmt = conn.prepareCall("BEGIN dbms_pipe.pack_message(?); ? := dbms_pipe.send_message(?,2); END;");
			
			// Manda para o pipe o status que representa que o
			// o deamon esta rodando.
			stmt.setInt(1, DEAMON_ALIVE);
			stmt.registerOutParameter(2, OracleTypes.NUMBER);			
			stmt.setString(3, pPipeReturn);
			
			stmt.execute();
			
			stmt.close();
			
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}	    
	}
}
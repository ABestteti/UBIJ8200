package br.com.acaosistemas.db.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import br.com.acaosistemas.db.connection.ConnectionFactory;
import br.com.acaosistemas.db.enumeration.StatusPoboxXMLEnum;
import br.com.acaosistemas.db.enumeration.TipoRecursoPoboxXMLEnum;
import br.com.acaosistemas.db.model.UBIPoboxXml;

public class UBIPoboxXmlDAO {

	private Connection conn;
	private UBIPoboxXml ubpx;
	
	public UBIPoboxXmlDAO() {
		conn = new ConnectionFactory().getConnection();
	}
	
	public void closeConnection () {
		try {
			conn.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			throw new RuntimeException(e);
		}
	}
	
	public UBIPoboxXml getUBIPoboxXML(String pRowID) {
		ubpx                   = new UBIPoboxXml();
		PreparedStatement stmt = null;
		
		try {
			stmt = conn.prepareStatement(
					"SELECT ubpx.rowid, ubpx.dt_mov, ubpx.status, ubpx.tipo_recurso, ubpx.ws_endpoint, ubpx.table_name, ubpx.nome_tapi, ubpx.sistema_remetente, ubpx.sistema_destinatario, ubpx.xml FROM ubi_pobox_xml ubpx WHERE ubpx.rowid = ?");
		
			stmt.setString(1, pRowID);
			
			ResultSet rs = stmt.executeQuery();
			
			while (rs.next()) {
				ubpx.setRowId(rs.getString("rowid"));
				ubpx.setId(rs.getTimestamp("dt_mov"));
				ubpx.setStatus(StatusPoboxXMLEnum.getById(rs.getInt("status")));
				ubpx.setTipoRecurso(TipoRecursoPoboxXMLEnum.getById(rs.getString("tipo_recurso")));
				ubpx.setWsEndpoint(rs.getString("ws_endpoint"));
				ubpx.setTableName(rs.getString("table_name"));
				ubpx.setNomeTapi(rs.getString("nome_tapi"));
				ubpx.setSistemaRemetente(rs.getString("sistema_remetente"));
				ubpx.setSistemaDestinatario(rs.getString("sistema_destinatario"));
				ubpx.setXml(rs.getString("xml"));
			}
			
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
		
		return ubpx;
	}
	
	/**
	 * Recupera todos os registros da tabela UBI_POBOX_XML cujo status seja
	 * nao processado (0).
	 * 
	 * @return
	 * Retorna uma lista com os registros retornados pela consulta.
	 */
	public List<UBIPoboxXml> listPoboxXml() {
		
		PreparedStatement stmt = null;
		List<UBIPoboxXml> lista = new ArrayList<UBIPoboxXml>();		
		try {
			stmt = conn.prepareStatement(
					"SELECT ubpx.rowid, ubpx.dt_mov, ubpx.status, ubpx.tipo_recurso, ubpx.ws_endpoint, ubpx.table_name, ubpx.nome_tapi, ubpx.sistema_remetente, ubpx.sistema_destinatario, ubpx.xml FROM ubi_pobox_xml ubpx WHERE ubpx.status = ?");
			
			stmt.setInt(1, StatusPoboxXMLEnum.NAO_PROCESSADO.getId());
			
			ResultSet rs = stmt.executeQuery();
			
			while (rs.next()) {
				UBIPoboxXml ubpx = new UBIPoboxXml();
				
				ubpx.setRowId(rs.getString("rowId"));
				ubpx.setId(rs.getTimestamp("dt_mov"));
				ubpx.setStatus(StatusPoboxXMLEnum.getById(rs.getInt("status")));
				ubpx.setTipoRecurso(TipoRecursoPoboxXMLEnum.getById(rs.getString("tipo_recurso")));
				ubpx.setWsEndpoint(rs.getString("ws_endpoint"));
				ubpx.setTableName(rs.getString("table_name"));
				ubpx.setNomeTapi(rs.getString("nome_tapi"));
				ubpx.setSistemaRemetente(rs.getString("sistema_remetente"));
				ubpx.setSistemaDestinatario(rs.getString("sistema_destinatario"));
				ubpx.setXml(rs.getString("xml"));
				
				lista.add(ubpx);
			}

		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
		
		return lista;
	}
}

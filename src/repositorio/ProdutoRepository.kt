package repositorio

import enums.Cor
import enums.Material
import models.produtos.CaixaDaAgua
import servico.RegistroNaoEncontradoException
import java.sql.Connection
import java.sql.ResultSet
import java.sql.Statement

/**
 * Acesso ao banco para a tabela `produto` (o catalogo de caixas d'agua e o estoque).
 * `entrarEstoque` / `baixarEstoque` recebem a `conn` da transacao (compra/venda) para
 * o estoque mudar junto com o caixa - tudo ou nada.
 */
object ProdutoRepository {

    /** Cadastra um produto novo (estoque inicial normalmente 0). Devolve o id gerado. */
    fun inserir(p: CaixaDaAgua): Int {
        val sql = """
            INSERT INTO produto (marca, modelo, cor, material, formato, largura, altura, profundidade,
                                 preco_venda, quantidade_estoque)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """
        Conexao.abrir().use { conn ->
            conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS).use { stmt ->
                preencher(stmt, p)
                stmt.setBigDecimal(9, p.precoVenda)
                stmt.setInt(10, p.quantidadeEstoque)
                stmt.executeUpdate()
                stmt.generatedKeys.use { rs ->
                    rs.next()
                    return rs.getInt(1)
                }
            }
        }
    }

    fun atualizar(p: CaixaDaAgua) {
        val id = p.id ?: throw RegistroNaoEncontradoException("Produto")
        val sql = """
            UPDATE produto SET marca = ?, modelo = ?, cor = ?, material = ?, formato = ?,
                   largura = ?, altura = ?, profundidade = ?, preco_venda = ?
            WHERE id = ?
        """
        Conexao.abrir().use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                preencher(stmt, p)
                stmt.setBigDecimal(9, p.precoVenda)
                stmt.setInt(10, id)
                if (stmt.executeUpdate() == 0) throw RegistroNaoEncontradoException("Produto #$id")
            }
        }
    }

    fun listar(): List<CaixaDaAgua> {
        val lista = mutableListOf<CaixaDaAgua>()
        Conexao.abrir().use { conn ->
            conn.prepareStatement("SELECT * FROM produto ORDER BY marca, modelo").use { stmt ->
                stmt.executeQuery().use { rs -> while (rs.next()) lista.add(mapear(rs)) }
            }
        }
        return lista
    }

    /** Busca fora de transacao (abre conexao propria). */
    fun buscar(id: Int): CaixaDaAgua? {
        Conexao.abrir().use { conn -> return buscar(conn, id) }
    }

    /** Mesma busca, mas reutilizando a conexao da transacao (para ler o estoque atual na venda). */
    fun buscar(conn: Connection, id: Int): CaixaDaAgua? {
        conn.prepareStatement("SELECT * FROM produto WHERE id = ?").use { stmt ->
            stmt.setInt(1, id)
            stmt.executeQuery().use { rs -> return if (rs.next()) mapear(rs) else null }
        }
    }

    /** Soma no estoque (usado na compra). O "+ ?" e feito pelo proprio banco. */
    fun entrarEstoque(conn: Connection, produtoId: Int, quantidade: Int) {
        conn.prepareStatement("UPDATE produto SET quantidade_estoque = quantidade_estoque + ? WHERE id = ?").use { stmt ->
            stmt.setInt(1, quantidade)
            stmt.setInt(2, produtoId)
            stmt.executeUpdate()
        }
    }

    /** Subtrai do estoque (usado na venda). O servico ja checou que ha estoque suficiente;
     *  o CHECK (quantidade_estoque >= 0) no banco e a ultima linha de defesa. */
    fun baixarEstoque(conn: Connection, produtoId: Int, quantidade: Int) {
        conn.prepareStatement("UPDATE produto SET quantidade_estoque = quantidade_estoque - ? WHERE id = ?").use { stmt ->
            stmt.setInt(1, quantidade)
            stmt.setInt(2, produtoId)
            stmt.executeUpdate()
        }
    }

    // Preenche os "?" 1..8 (campos comuns ao INSERT e ao UPDATE).
    private fun preencher(stmt: java.sql.PreparedStatement, p: CaixaDaAgua) {
        stmt.setString(1, p.marca)
        stmt.setString(2, p.modelo)
        stmt.setString(3, p.cor.name)
        stmt.setString(4, p.material.name)
        stmt.setString(5, p.formato)
        stmt.setDouble(6, p.largura)
        stmt.setDouble(7, p.altura)
        stmt.setDouble(8, p.profundidade)
    }

    private fun mapear(rs: ResultSet) = CaixaDaAgua(
        id = rs.getInt("id"),
        marca = rs.getString("marca"),
        modelo = rs.getString("modelo"),
        cor = Cor.valueOf(rs.getString("cor")),
        material = Material.valueOf(rs.getString("material")),
        formato = rs.getString("formato"),
        largura = rs.getDouble("largura"),
        altura = rs.getDouble("altura"),
        profundidade = rs.getDouble("profundidade"),
        precoVenda = rs.getBigDecimal("preco_venda"),
        quantidadeEstoque = rs.getInt("quantidade_estoque")
    )
}

package servico

import java.math.BigDecimal

/** Erros de regra de negocio. O menu captura essas excecoes e mostra uma mensagem amigavel. */

class RegistroNaoEncontradoException(oQue: String) :
    RuntimeException("$oQue nao encontrado(a).")

class EstoqueInsuficienteException(disponivel: Int, pedido: Int) :
    RuntimeException("Estoque insuficiente: disponivel $disponivel, pedido $pedido.")

class SaldoInsuficienteException(saldo: BigDecimal, necessario: BigDecimal) :
    RuntimeException("Saldo insuficiente no caixa: saldo $saldo, necessario $necessario.")

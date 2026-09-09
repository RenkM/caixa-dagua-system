package sistema

import enums.Habilidade
import enums.Turno
import models.pessoas.Auditor
import models.pessoas.Cliente
import models.pessoas.Fornecedor
import models.pessoas.Funcionario
import repositorio.AuditorRepository
import repositorio.ClienteRepository
import repositorio.FornecedorRepository
import repositorio.FuncionarioRepository
import repositorio.SetorRepository
import util.Entrada
import java.math.BigDecimal

/**
 * Submenu de PESSOAS: cadastra e lista funcionarios, clientes, fornecedores e auditores,
 * e lista funcionarios por setor. Cada acao roda dentro de `executarSeguro { }` (ver Menu.kt),
 * entao erro nao derruba o menu.
 */
object MenuPessoas {

    fun abrir() {
        while (true) {
            println(
                """
                |
                |--- PESSOAS ---
                |1 - Cadastrar funcionario
                |2 - Cadastrar cliente
                |3 - Cadastrar fornecedor
                |4 - Cadastrar auditor
                |5 - Listar funcionarios
                |6 - Listar funcionarios por setor
                |7 - Listar clientes
                |8 - Listar fornecedores
                |9 - Listar auditores
                |0 - Voltar
                """.trimMargin()
            )
            when (Entrada.lerTexto("Opcao")) {
                "1" -> executarSeguro(::cadastrarFuncionario)
                "2" -> executarSeguro(::cadastrarCliente)
                "3" -> executarSeguro(::cadastrarFornecedor)
                "4" -> executarSeguro(::cadastrarAuditor)
                "5" -> executarSeguro { FuncionarioRepository.listar().forEach(::println) }
                "6" -> executarSeguro(::listarFuncionariosPorSetor)
                "7" -> executarSeguro { ClienteRepository.listar().forEach(::println) }
                "8" -> executarSeguro { FornecedorRepository.listar().forEach(::println) }
                "9" -> executarSeguro { AuditorRepository.listar().forEach(::println) }
                "0" -> return
                else -> println("Opcao invalida.")
            }
        }
    }

    private fun cadastrarFuncionario() {
        // cada leitura ja valida sozinha e repete ate o dado ficar certo
        val nome = Entrada.lerTexto("Nome")
        val sobrenome = Entrada.lerTexto("Sobrenome")
        val cpf = Entrada.lerCpf()
        val rg = Entrada.lerRg()
        val idade = Entrada.lerIdade()
        val salario = Entrada.lerBigDecimal("Salario", BigDecimal.ZERO)
        val turno = Entrada.escolherEnum("Turno:", Turno.values())
        val habilidade = Entrada.escolherEnum("Habilidade:", Habilidade.values())

        // funcionario precisa de um setor: mostra os setores do banco para escolher
        val setores = SetorRepository.listar()
        val setor = Entrada.escolherDaLista("Setor:", setores) { it.nome }

        val id = FuncionarioRepository.inserir(
            Funcionario(null, nome, sobrenome, cpf, rg, idade, salario, turno, habilidade, setor.id, setor.nome)
        )
        println("Funcionario #$id cadastrado no setor ${setor.nome}.")
    }

    private fun cadastrarCliente() {
        val nome = Entrada.lerTexto("Nome")
        val sobrenome = Entrada.lerTexto("Sobrenome")
        val cpf = Entrada.lerCpf()
        val rg = Entrada.lerRg()
        val idade = Entrada.lerIdade()
        val id = ClienteRepository.inserir(Cliente(null, nome, sobrenome, cpf, rg, idade, dividasAbertas = false))
        println("Cliente #$id cadastrado.")
    }

    private fun cadastrarFornecedor() {
        val nomeFantasia = Entrada.lerTexto("Nome fantasia")
        val cnpj = Entrada.lerCnpj()
        val telefone = Entrada.lerTelefoneOpcional()
        val email = Entrada.lerEmailOpcional()
        val id = FornecedorRepository.inserir(Fornecedor(null, nomeFantasia, cnpj, telefone, email))
        println("Fornecedor #$id cadastrado.")
    }

    private fun cadastrarAuditor() {
        val nome = Entrada.lerTexto("Nome")
        val sobrenome = Entrada.lerTexto("Sobrenome")
        val cpf = Entrada.lerCpf()
        val rg = Entrada.lerRg()
        val idade = Entrada.lerIdade()
        val registro = Entrada.lerTexto("Registro profissional")
        val orgao = Entrada.lerTexto("Orgao emissor")
        val id = AuditorRepository.inserir(Auditor(null, nome, sobrenome, cpf, rg, idade, registro, orgao))
        println("Auditor #$id cadastrado.")
    }

    private fun listarFuncionariosPorSetor() {
        val setores = SetorRepository.listar()
        val setor = Entrada.escolherDaLista("Setor:", setores) { it.nome }
        val funcionarios = FuncionarioRepository.listarPorSetor(setor.id)
        if (funcionarios.isEmpty()) {
            println("Nenhum funcionario no setor ${setor.nome}.")
        } else {
            println("Funcionarios do setor ${setor.nome}:")
            funcionarios.forEach(::println)
        }
    }
}

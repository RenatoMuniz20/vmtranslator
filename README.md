# VMTranslator

Tradutor da linguagem VM para Assembly Hack (Nand2Tetris, Projects 07 e 08).

---

## 👥 Créditos

**Alunos:**

| Nome completo | Matrícula |
|---|---|
| José Nunes de Sousa Neto | 20250071231 |
| Renato Muniz Gomes | 20250071330 |

**Disciplina:** EECP0026 — Compiladores

**Professor:** Prof. Dr. Sergio Souza Costa

**Instituição:** UFMA — Universidade Federal do Maranhão

**Semestre:** 2026.2

---

## Linguagem

- Java 17 (compatível com versões mais novas)
- Maven 3.x para build e dependências
- JUnit 5 para testes unitários

## Build

```bash
mvn package
```

Gera `target/vmtranslator.jar`.

## Execução

```bash
# um arquivo .vm  → gera Xxx.asm na mesma pasta
java -jar target/vmtranslator.jar caminho/Xxx.vm

# um diretório    → gera caminho/Xxx/Xxx.asm
java -jar target/vmtranslator.jar caminho/Xxx
```

---

<div align="center">

**Este repositório possui fins acadêmicos.**

</div>
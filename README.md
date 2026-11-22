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

Em caso de erro no `.vm`, o tradutor informa a linha e o comando, por exemplo
`Erro: Linha 12: comando desconhecido -> "mul"`, e não gera o `.asm`.

## Estrutura do código

| Pacote | Responsabilidade |
|---|---|
| `vmtranslator.parser` | `Parser`: lê o `.vm`, remove comentários e espaços e decompõe cada comando (`commandType`, `arg1`, `arg2`). `CommandType`: enum dos tipos de comando. `VMSyntaxException`: erro com número da linha. |
| `vmtranslator.codewriter` | `CodeWriter`: gera o Assembly Hack de cada comando. |
| `vmtranslator.main` | `Main`: valida a entrada e liga o `Parser` ao `CodeWriter`. |

## Funcionalidades implementadas

**Parte 1 (Project 07)**

- Comandos aritméticos e lógicos: `add`, `sub`, `neg`, `eq`, `gt`, `lt`, `and`, `or`, `not`
  (verdadeiro = `-1`, falso = `0`; cada comparação usa um rótulo único `TRUE_<op>_<n>`)
- `push`/`pop` nos segmentos `constant`, `local`, `argument`, `this`, `that`, `temp`, `pointer` e `static`
- Cada comando VM é escrito como comentário no `.asm` (ex.: `// push constant 17`)
- Mensagens de erro com o número da linha do `.vm`

**Parte 2 (Project 08)**: em desenvolvimento.

## Testes

### Testes unitários (JUnit)

```bash
mvn test
```

### Testes do Nand2Tetris (CPU Emulator)

Os scripts traduzem cada teste listado em `scripts/tests.txt` e o executam no CPU Emulator,
informando se a comparação com o `.cmp` passou.

```powershell
# Windows (PowerShell)
powershell -ExecutionPolicy Bypass -File .\scripts\run-tests.ps1 -ToolsDir C:\nand2tetris\tools
```

```bash
# Linux / Mac
NAND2TETRIS_TOOLS=~/nand2tetris/tools scripts/run-tests.sh
```

### Resultados

| Teste | Project | Resultado |
|---|---|---|
| StackArithmetic/SimpleAdd | 07 | ✅ passou |
| StackArithmetic/StackTest | 07 | ✅ passou |
| MemoryAccess/BasicTest | 07 | ✅ passou |
| MemoryAccess/PointerTest | 07 | ✅ passou |
| MemoryAccess/StaticTest | 07 | ✅ passou |
| ProgramFlow/BasicLoop | 08 | ⏳ pendente |
| ProgramFlow/FibonacciSeries | 08 | ⏳ pendente |
| FunctionCalls/SimpleFunction | 08 | ⏳ pendente |
| FunctionCalls/NestedCall | 08 | ⏳ pendente |
| FunctionCalls/FibonacciElement | 08 | ⏳ pendente |
| FunctionCalls/StaticsTest | 08 | ⏳ pendente |

---

<div align="center">

**Este repositório possui fins acadêmicos.**

</div>

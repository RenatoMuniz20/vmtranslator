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

Exemplos com os testes do repositório:

```bash
java -jar target/vmtranslator.jar projects/7/StackArithmetic/SimpleAdd/SimpleAdd.vm
java -jar target/vmtranslator.jar projects/8/FunctionCalls/FibonacciElement
```

Ao traduzir um diretório, todos os `.vm` são traduzidos em ordem alfabética para um único `.asm`.
O código de bootstrap (`SP = 256` e `call Sys.init 0`) é gerado **somente** quando o diretório
contém `Sys.vm`.

Em caso de erro no `.vm`, o tradutor informa o arquivo, a linha e o comando, por exemplo
`Erro: Main.vm, Linha 12: comando desconhecido -> "mul"`, e não gera o `.asm`.

## Estrutura do código

| Pacote | Responsabilidade |
|---|---|
| `vmtranslator.parser` | `Parser`: lê o `.vm`, remove comentários e espaços e decompõe cada comando (`commandType`, `arg1`, `arg2`). `CommandType`: enum dos tipos de comando. `VMSyntaxException`: erro com número da linha. |
| `vmtranslator.codewriter` | `CodeWriter`: gera o Assembly Hack de cada comando. |
| `vmtranslator.main` | `Main`: valida a entrada (arquivo ou diretório), decide se gera o bootstrap e liga o `Parser` ao `CodeWriter`. |

## Funcionalidades implementadas

**Parte 1 (Project 07)**

- Comandos aritméticos e lógicos: `add`, `sub`, `neg`, `eq`, `gt`, `lt`, `and`, `or`, `not`
  (verdadeiro = `-1`, falso = `0`; cada comparação usa um rótulo único `TRUE_<op>_<n>`)
- `push`/`pop` nos segmentos `constant`, `local`, `argument`, `this`, `that`, `temp`, `pointer` e `static`
- Cada comando VM é escrito como comentário no `.asm` (ex.: `// push constant 17`)
- Mensagens de erro com o número da linha do `.vm`

**Parte 2 (Project 08)**

- Controle de fluxo: `label`, `goto`, `if-goto`. Os rótulos recebem o nome da função corrente
  (`função$rótulo`), então duas funções podem ter, cada uma, o seu `label LOOP`
- `function f nVars`: declara `(f)` e inicializa as `nVars` variáveis locais com `0`
- `call f nArgs`: empilha o endereço de retorno e `LCL`, `ARG`, `THIS`, `THAT`; faz `ARG = SP - 5 - nArgs`
  e `LCL = SP`; cada chamada tem o seu rótulo de retorno único (`chamador$ret.i`)
- `return`: guarda `endFrame` em `R13` e o endereço de retorno em `R14` **antes** de gravar o valor
  de retorno em `*ARG` (necessário para funções com 0 argumentos), restaura o frame do chamador e salta
- Bootstrap (`writeInit`): `SP = 256` e `call Sys.init 0`, gerado só para diretórios com `Sys.vm`
- Tradução de diretórios, com os símbolos `static` separados por arquivo (`Class1.0`, `Class2.0`)

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
| ProgramFlow/BasicLoop | 08 | ✅ passou |
| ProgramFlow/FibonacciSeries | 08 | ✅ passou |
| FunctionCalls/SimpleFunction | 08 | ✅ passou |
| FunctionCalls/NestedCall | 08 | ✅ passou |
| FunctionCalls/FibonacciElement | 08 | ✅ passou |
| FunctionCalls/StaticsTest | 08 | ✅ passou |

---

<div align="center">

**Este repositório possui fins acadêmicos.**

</div>

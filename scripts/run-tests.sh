#!/usr/bin/env bash
# Traduz e roda no CPU Emulator todos os testes listados em scripts/tests.txt.
#
# Uso (a partir da raiz do repositorio):
#   NAND2TETRIS_TOOLS=~/nand2tetris/tools scripts/run-tests.sh

cd "$(dirname "$0")/.." || exit 2

TOOLS="${NAND2TETRIS_TOOLS:-}"
if [ -z "$TOOLS" ] || [ ! -f "$TOOLS/CPUEmulator.sh" ]; then
    echo "Defina NAND2TETRIS_TOOLS com a pasta tools do Nand2Tetris (onde fica CPUEmulator.sh)." >&2
    exit 2
fi

echo "Compilando..."
mvn -q package || { echo "Falha no build." >&2; exit 2; }

passed=0
failed=0
while IFS= read -r line || [ -n "$line" ]; do
    line="${line%%#*}"              # remove comentarios
    read -r entry _ <<< "$line"     # remove espacos
    [ -z "$entry" ] && continue
    entry="${entry%/}"

    if [[ "$entry" == *.vm ]]; then
        tst="${entry%.vm}.tst"
    else
        tst="$entry/$(basename "$entry").tst"
    fi
    name="$(basename "$tst" .tst)"

    if ! java -jar target/vmtranslator.jar "$entry" > /dev/null; then
        status="FALHOU (traducao)"
    else
        output="$(bash "$TOOLS/CPUEmulator.sh" "$PWD/$tst" 2>&1)"
        if grep -q "Comparison ended successfully" <<< "$output"; then
            status="PASSOU"
        else
            status="FALHOU: $(grep -v '^\s*$' <<< "$output" | tail -n 1)"
        fi
    fi

    if [ "$status" = "PASSOU" ]; then passed=$((passed + 1)); else failed=$((failed + 1)); fi
    printf "%-18s %s\n" "$name" "$status"
done < scripts/tests.txt

echo
echo "Passaram: $passed   Falharam: $failed"
[ "$failed" -eq 0 ]

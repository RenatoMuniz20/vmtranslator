<#
.SYNOPSIS
    Traduz e roda no CPU Emulator todos os testes listados em scripts\tests.txt.

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File .\scripts\run-tests.ps1 -ToolsDir C:\nand2tetris\tools

.NOTES
    Em vez de -ToolsDir, tambem e possivel definir a variavel de ambiente NAND2TETRIS_TOOLS.
#>
param(
    [string]$ToolsDir = $env:NAND2TETRIS_TOOLS
)

Set-Location (Join-Path $PSScriptRoot '..')

$emulator = $null
if ($ToolsDir) { $emulator = Join-Path $ToolsDir 'CPUEmulator.bat' }
if (-not $emulator -or -not (Test-Path $emulator)) {
    Write-Host 'Informe a pasta tools do Nand2Tetris, por exemplo:' -ForegroundColor Red
    Write-Host '  .\scripts\run-tests.ps1 -ToolsDir C:\nand2tetris\tools' -ForegroundColor Red
    exit 2
}

Write-Host 'Compilando...'
mvn -q package
if ($LASTEXITCODE -ne 0) {
    Write-Host 'Falha no build.' -ForegroundColor Red
    exit 2
}

$passed = 0
$failed = 0
foreach ($line in Get-Content 'scripts\tests.txt') {
    $entry = ($line -replace '#.*$', '').Trim()     # remove comentarios e espacos
    if (-not $entry) { continue }
    $entry = $entry.TrimEnd('/', '\')

    if ($entry.EndsWith('.vm')) {
        $tst = $entry.Substring(0, $entry.Length - 3) + '.tst'
    } else {
        $tst = Join-Path $entry ((Split-Path $entry -Leaf) + '.tst')
    }
    $name = [IO.Path]::GetFileNameWithoutExtension($tst)

    java -jar target\vmtranslator.jar $entry | Out-Null
    if ($LASTEXITCODE -ne 0) {
        $status = 'FALHOU (traducao)'
    } else {
        $tstFullPath = (Resolve-Path $tst).Path
        $output = (& $emulator $tstFullPath 2>&1 | Out-String)
        if ($output -match 'Comparison ended successfully') {
            $status = 'PASSOU'
        } else {
            $lastLine = ($output -split "`r?`n" | Where-Object { $_.Trim() } | Select-Object -Last 1)
            $status = "FALHOU: $lastLine"
        }
    }

    if ($status -eq 'PASSOU') { $passed++; $color = 'Green' } else { $failed++; $color = 'Red' }
    Write-Host ('{0,-18} {1}' -f $name, $status) -ForegroundColor $color
}

Write-Host ''
Write-Host "Passaram: $passed   Falharam: $failed"
if ($failed -gt 0) { exit 1 } else { exit 0 }

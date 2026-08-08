$root = Split-Path -Parent $PSScriptRoot
$descPath = Join-Path $root "docs/class-descriptions.json"
$examplesPath = Join-Path $root "docs/class-examples.json"
$desc = Get-Content -Raw -Encoding UTF8 $descPath | ConvertFrom-Json
$dot = [string][char]0xB7
$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
$allFiles = @(Get-ChildItem (Join-Path $root "src/main") -Recurse -Include *.kt, *.java | Sort-Object FullName)

$classRe = '(?:class|object|interface|fun interface|sealed interface|enum class|data class|sealed class|abstract class|public class|public interface|public abstract class|public final class)\s+([A-Za-z0-9_]+)'
$classFile = @{}
foreach ($f in $allFiles) {
    $m = [regex]::Match([System.IO.File]::ReadAllText($f.FullName), $classRe)
    if ($m.Success -and -not $classFile.ContainsKey($m.Groups[1].Value)) {
        $classFile[$m.Groups[1].Value] = $f.FullName
    }
}

$usage = @{}
if (Test-Path $examplesPath) {
    $stored = Get-Content -Raw -Encoding UTF8 $examplesPath | ConvertFrom-Json
    foreach ($prop in $stored.PSObject.Properties) {
        $usage[$prop.Name] = $prop.Value
    }
}

$manualExamples = @{
    'LibGltfClient' = 'object LibGltfClient : ClientModInitializer'
    'AnimationStateMachineBuilder' = 'AnimationStateMachineBuilder(asset).floatParameter("speed")'
    'GltfRenderers' = 'GltfRenderers.item(instance)'
    'IrisMixinPlugin' = 'class IrisMixinPlugin : IMixinConfigPlugin'
    'GltfLoadResult' = 'val result: GltfLoadResult = api.loadAsync(path).get()'
    'GltfItemInstanceProvider' = 'GltfRenderers.item(instance, provider)'
    'VulkanMeshRenderPass' = '(renderPass as VulkanMeshRenderPass).drawMeshTasks(cache, pipeline, ...)'
    'GltfRenderSystem' = 'GltfRenderSystem.initialize()'
}

$callPattern = [regex]'\b([A-Z][A-Za-z0-9_]*)(?:\s*\.\s*[A-Za-z0-9_]+)?\s*\('
$refPattern = [regex]'\b([A-Z][A-Za-z0-9_]*)\b'

foreach ($f in $allFiles) {
    $lines = [System.IO.File]::ReadAllText($f.FullName) -split "`r?`n"
    for ($i = 0; $i -lt $lines.Count; $i++) {
        $t = $lines[$i].Trim()
        if ($t.StartsWith('*') -or $t.StartsWith('//') -or $t.StartsWith('/*') -or $t.StartsWith('import ') -or $t.StartsWith('package ')) {
            continue
        }
        $callHit = $null
        foreach ($m in $callPattern.Matches($lines[$i])) {
            $name = $m.Groups[1].Value
            if ($classFile.ContainsKey($name) -and $classFile[$name] -ne $f.FullName -and -not $usage.ContainsKey($name)) {
                $callHit = $name
                break
            }
        }
        if ($callHit) {
            $balance = 0
            $snippet = @()
            for ($j = $i; $j -lt [Math]::Min($lines.Count, $i + 14); $j++) {
                $sl = $lines[$j].Trim()
                $balance += ([regex]::Matches($sl, '\(').Count) - ([regex]::Matches($sl, '\)').Count)
                $snippet += $sl
                if ($balance -le 0) { break }
            }
            $usage[$callHit] = ($snippet -join "`n")
            continue
        }
        foreach ($m in $refPattern.Matches($lines[$i])) {
            $name = $m.Groups[1].Value
            if ($classFile.ContainsKey($name) -and $classFile[$name] -ne $f.FullName -and -not $usage.ContainsKey($name)) {
                $usage[$name] = $t
                break
            }
        }
    }
}

foreach ($f in (Get-ChildItem (Join-Path $root "src/main") -Recurse -Include *.java | Sort-Object FullName)) {
    $lines = [System.IO.File]::ReadAllText($f.FullName) -split "`r?`n"
    foreach ($line in $lines) {
        $mixin = [regex]::Match($line, '@Mixin\([^)]*\)')
        if ($mixin.Success) {
            $decl = [regex]::Match([System.IO.File]::ReadAllText($f.FullName), '(?:class|interface)\s+([A-Za-z0-9_]+)')
            if ($decl.Success -and -not $usage.ContainsKey($decl.Groups[1].Value)) {
                $usage[$decl.Groups[1].Value] = $mixin.Value
            }
            break
        }
    }
}

foreach ($entry in $manualExamples.GetEnumerator()) {
    if (-not $usage.ContainsKey($entry.Key)) {
        $usage[$entry.Key] = $entry.Value
    }
}

$ordered = [ordered]@{}
foreach ($key in ($usage.Keys | Sort-Object)) {
    $ordered[$key] = $usage[$key]
}
$json = $ordered | ConvertTo-Json -Depth 2
[System.IO.File]::WriteAllText($examplesPath, $json, $utf8NoBom)

$old = '(?ms)^/\*\*\r?\n \* libgltf[^\r\n]*\r?\n(?: \*[^\r\n]*\r?\n)*? \* @author Chen Micheanl\r?\n \* @license MIT\r?\n \* @see \[Micheanl/libglTF\]\(https://github\.com/Micheanl/libglTF\)\r?\n \*/\r?\n'
$count = 0
$withExample = 0
foreach ($f in (Get-ChildItem (Join-Path $root "src/main") -Recurse -Include *.kt, *.java | Sort-Object FullName)) {
    $text = [System.IO.File]::ReadAllText($f.FullName)
    $text = [regex]::Replace($text, $old, '')
    $classMatch = [regex]::Match($text, '(?m)^(?:public\s+)?(?:abstract\s+|final\s+)?(?:class|object|interface|fun interface|sealed interface|enum class|data class|sealed class)\s+([A-Za-z0-9_]+)')
    if (-not $classMatch.Success) { continue }
    $className = $classMatch.Groups[1].Value
    $lines = @("/**", " * libgltf $dot $className")
    if ($usage.ContainsKey($className)) {
        $lines += ' *'
        $lines += ' * ```'
        foreach ($codeLine in ($usage[$className] -split "`n")) {
            $lines += " * $codeLine"
        }
        $lines += ' * ```'
        $withExample++
    }
    $lines += ' *'
    $lines += " * $($desc.$className)"
    $lines += @(' *', ' * @author Chen Micheanl', ' * @license MIT', ' * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)', ' */', '')
    $text = $text.Substring(0, $classMatch.Index) + ($lines -join "`r`n") + "`r`n" + $text.Substring($classMatch.Index)
    [System.IO.File]::WriteAllText($f.FullName, $text, $utf8NoBom)
    $count++
}
Write-Host "updated $count files, $withExample with examples"

#requires -Version 5.1
<#
  Regenerates the optional 1.7.10 texture-fix resource pack on the LOCAL machine.

  Nothing generated here is committed to the repository: recolored vanilla textures stay under the
  Minecraft EULA and mod textures stay under their authors' licenses, so the pack is rebuilt from
  assets you already have installed instead of being redistributed.

  What it fixes (log lines like "domain minecraft is missing 19 textures"):
    * Thaumic Tinkerer registers its focus items with bare texture names, so they resolve to
      minecraft:textures/items/focus*.png - the real art is copied from the Thaumic Tinkerer jar.
    * RotaryCraft's steel/bedrock tools have no plain item icons - recolored vanilla iron tools
      are used as stand-ins.
    * A few block/item icons of Compacter, Ex-Astris, Extra Utilities and TConstruct-adjacent
      parts are filled from the nearest texture in the owning jar.

  Usage:
    powershell -NoProfile -ExecutionPolicy Bypass -File build-texturepack.ps1 `
        -GameDir E:\work\mc1710 -ModsDir E:\work\mc1710\mods -VanillaJar D:\Minecraft\.minecraft\versions\1.7.10\1.7.10.jar
#>
param(
    [Parameter(Mandatory = $true)][string]$GameDir,
    [Parameter(Mandatory = $true)][string]$ModsDir,
    [Parameter(Mandatory = $true)][string]$VanillaJar,
    [string]$PackName = "forge1710-texturefix"
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression.FileSystem
Add-Type -AssemblyName System.Drawing

$pack = Join-Path $GameDir "resourcepacks\$PackName"
$tmp = Join-Path $env:TEMP "mend-texturepack-tmp"
if (Test-Path $pack) { Remove-Item $pack -Recurse -Force }
if (Test-Path $tmp) { Remove-Item $tmp -Recurse -Force }
New-Item -ItemType Directory -Force -Path $tmp | Out-Null

$dirs = @(
    "assets\minecraft\textures\items",
    "assets\minecraft\textures\blocks",
    "assets\ttinkerer\textures\items",
    "assets\ttinkerer\textures\blocks",
    "assets\compacter\textures\blocks",
    "assets\exastris\textures\blocks",
    "assets\extrautils\textures\items",
    "assets\missing_icon_item_4650_extrautils\textures\items"
)
foreach ($d in $dirs) { New-Item -ItemType Directory -Force -Path (Join-Path $pack $d) | Out-Null }

function Find-Mod([string]$pattern) {
    $f = Get-ChildItem $ModsDir -Filter $pattern -ErrorAction SilentlyContinue | Select-Object -First 1
    if (-not $f) { throw "required mod jar not found: $pattern in $ModsDir" }
    return $f.FullName
}

function Extract-Entry([string]$jar, [string]$entry, [string]$dest) {
    $zip = [IO.Compression.ZipFile]::OpenRead($jar)
    try {
        $e = $zip.Entries | Where-Object { $_.FullName -eq $entry } | Select-Object -First 1
        if (-not $e) { return $false }
        [IO.Compression.ZipFileExtensions]::ExtractToFile($e, $dest, $true)
        return $true
    } finally {
        $zip.Dispose()
    }
}

function Tint([string]$src, [string]$dst, [int]$r, [int]$g, [int]$b) {
    $img = [System.Drawing.Image]::FromFile($src)
    try {
        $bmp = New-Object System.Drawing.Bitmap($img.Width, $img.Height)
        for ($y = 0; $y -lt $img.Height; $y++) {
            for ($x = 0; $x -lt $img.Width; $x++) {
                $c = $img.GetPixel($x, $y)
                $bmp.SetPixel($x, $y, [System.Drawing.Color]::FromArgb($c.A, [int]($c.R * $r / 255), [int]($c.G * $g / 255), [int]($c.B * $b / 255)))
            }
        }
        $bmp.Save($dst, [System.Drawing.Imaging.ImageFormat]::Png)
        $bmp.Dispose()
    } finally {
        $img.Dispose()
    }
}

$tt = Find-Mod '*ThaumicTinkerer*.jar'
$cp = Find-Mod 'compacter-*.jar'
$ex = Find-Mod 'Ex-Astris-*.jar'
$eu = Find-Mod 'extrautilities-*.jar'

# 1) Thaumic Tinkerer foci: real art, just moved into the minecraft domain
$foci = 'focusHeal', 'focusDeflect', 'focusTelekinesis', 'focusDislocation', 'focusFlight',
        'focusShadowbeam', 'focusEnderChest', 'focusXPDrain', 'focusSmelt', 'focusRecall'
$copiedFoci = 0
foreach ($f in $foci) {
    if (Extract-Entry $tt "assets/ttinkerer/textures/items/$f.png" (Join-Path $pack "assets\minecraft\textures\items\$f.png")) { $copiedFoci++ }
}
Write-Host "foci copied: $copiedFoci / $($foci.Count)"

# 2) RotaryCraft steel/bedrock tools: recolored vanilla iron tools
Extract-Entry $VanillaJar 'assets/minecraft/textures/items/iron_sword.png' (Join-Path $tmp 'iron_sword.png') | Out-Null
Extract-Entry $VanillaJar 'assets/minecraft/textures/items/iron_hoe.png' (Join-Path $tmp 'iron_hoe.png') | Out-Null
Extract-Entry $VanillaJar 'assets/minecraft/textures/items/shears.png' (Join-Path $tmp 'shears.png') | Out-Null
Tint (Join-Path $tmp 'iron_sword.png') (Join-Path $pack 'assets\minecraft\textures\items\item.steelsword.png') 150 165 175
Tint (Join-Path $tmp 'iron_hoe.png') (Join-Path $pack 'assets\minecraft\textures\items\item.steelhoe.png') 150 165 175
Tint (Join-Path $tmp 'shears.png') (Join-Path $pack 'assets\minecraft\textures\items\item.steelshears.png') 150 165 175
Tint (Join-Path $tmp 'iron_sword.png') (Join-Path $pack 'assets\minecraft\textures\items\item.bedrocksword.png') 55 50 60
Tint (Join-Path $tmp 'iron_hoe.png') (Join-Path $pack 'assets\minecraft\textures\items\item.bedrockhoe.png') 55 50 60
Tint (Join-Path $tmp 'shears.png') (Join-Path $pack 'assets\minecraft\textures\items\item.bedrockshears.png') 55 50 60

# 3) misc icons
Extract-Entry $VanillaJar 'assets/minecraft/textures/items/paper.png' (Join-Path $tmp 'paper.png') | Out-Null
Tint (Join-Path $tmp 'paper.png') (Join-Path $pack 'assets\minecraft\textures\items\itemAspect.png') 160 130 210
Extract-Entry $VanillaJar 'assets/minecraft/textures/blocks/planks_oak.png' (Join-Path $pack 'assets\minecraft\textures\blocks\planks.png') | Out-Null

# 4) generated bone skull
$bmp = New-Object System.Drawing.Bitmap 16, 16
$gfx = [System.Drawing.Graphics]::FromImage($bmp)
$gfx.Clear([System.Drawing.Color]::FromArgb(255, 214, 208, 190))
$brush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 45, 40, 35))
$gfx.FillEllipse($brush, 3, 6, 4, 4)
$gfx.FillEllipse($brush, 9, 6, 4, 4)
$gfx.FillRectangle($brush, 7, 9, 2, 3)
$bmp.Save((Join-Path $pack 'assets\minecraft\textures\blocks\gaiaHeadBlock_skeleton.png'), [System.Drawing.Imaging.ImageFormat]::Png)
$gfx.Dispose(); $bmp.Dispose()

# 5) neighbouring art from the owning jars
Extract-Entry $tt 'assets/ttinkerer/textures/items/ichorPickGem2.png' (Join-Path $pack 'assets\ttinkerer\textures\items\ichorPickGem.png') | Out-Null
foreach ($n in 'ichorSwordGem', 'ichorAxeGem', 'ichorShovelGem', 'mobAspect') {
    Copy-Item (Join-Path $pack 'assets\ttinkerer\textures\items\ichorPickGem.png') (Join-Path $pack "assets\ttinkerer\textures\items\$n.png") -Force
}
Extract-Entry $tt 'assets/ttinkerer/textures/model/repairer.png' (Join-Path $pack 'assets\ttinkerer\textures\blocks\repairer.png') | Out-Null
Extract-Entry $tt 'assets/ttinkerer/textures/model/magnetN.png' (Join-Path $pack 'assets\ttinkerer\textures\blocks\magnet.png') | Out-Null
Extract-Entry $cp 'assets/compacter/textures/blocks/compacter.png' (Join-Path $pack 'assets\compacter\textures\blocks\cobbler.png') | Out-Null
Extract-Entry $ex 'assets/exastris/textures/blocks/IconBarrelThaumiumCompost.png' (Join-Path $pack 'assets\exastris\textures\blocks\IconBarrelInternalClouds.png') | Out-Null
Extract-Entry $eu 'assets/extrautils/textures/items/ethericsword.png' (Join-Path $pack 'assets\extrautils\textures\items\lawSword.png') | Out-Null
Copy-Item (Join-Path $pack 'assets\extrautils\textures\items\lawSword.png') (Join-Path $pack 'assets\missing_icon_item_4650_extrautils\textures\items\lawSword.png') -Force

# 6) pack metadata
'{"pack":{"pack_format":1,"description":"Mend texture fix (generated locally)"}}' | Set-Content (Join-Path $pack 'pack.mcmeta') -Encoding ASCII

# 7) select it in options.txt
$opt = Join-Path $GameDir 'options.txt'
if (Test-Path $opt) {
    $content = Get-Content $opt -Raw
    if ($content -match '(?m)^resourcePacks:') {
        $content = $content -replace '(?m)^resourcePacks:.*$', "resourcePacks:[""$PackName""]"
    } else {
        $content = $content.TrimEnd() + "`r`nresourcePacks:[""$PackName""]`r`n"
    }
    Set-Content $opt $content -Encoding ASCII
}

$files = (Get-ChildItem $pack -Recurse -File).Count
Write-Host "pack written: $pack ($files files)"

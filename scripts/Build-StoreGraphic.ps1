param(
    [string]$Screenshot = (Join-Path $PSScriptRoot '..\store\assets\phone\02-forecast.png'),
    [int]$CropTop = 750,
    [int]$CropHeight = 300,
    [string]$Output = (Join-Path $PSScriptRoot '..\store\assets\feature-graphic.png')
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$image = [Drawing.Image]::FromFile([IO.Path]::GetFullPath($Screenshot))
$bitmap = [Drawing.Bitmap]::new(1024, 500, [Drawing.Imaging.PixelFormat]::Format24bppRgb)
$graphics = [Drawing.Graphics]::FromImage($bitmap)
$title = [Drawing.Font]::new('Bahnschrift', 76, [Drawing.FontStyle]::Bold, [Drawing.GraphicsUnit]::Pixel)
$subtitle = [Drawing.Font]::new('Bahnschrift', 27, [Drawing.FontStyle]::Regular, [Drawing.GraphicsUnit]::Pixel)
$ink = [Drawing.SolidBrush]::new([Drawing.ColorTranslator]::FromHtml('#20242A'))
$secondary = [Drawing.SolidBrush]::new([Drawing.ColorTranslator]::FromHtml('#46505B'))
$accent = [Drawing.SolidBrush]::new([Drawing.ColorTranslator]::FromHtml('#1E6FE0'))
try {
    if ($CropTop -lt 0 -or $CropTop + $CropHeight -gt $image.Height) { throw 'Feature graphic crop exceeds source image' }
    $graphics.Clear([Drawing.ColorTranslator]::FromHtml('#F5F7FD'))
    $graphics.TextRenderingHint = [Drawing.Text.TextRenderingHint]::AntiAliasGridFit
    $graphics.InterpolationMode = [Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $graphics.DrawString('Tropo', $title, $ink, 48, 26)
    $graphics.DrawString('Weather, air quality and a ten-day outlook', $subtitle, $secondary, 52, 122)
    $graphics.FillRectangle($accent, 52, 179, 68, 4)
    $source = [Drawing.Rectangle]::new(0, $CropTop, $image.Width, $CropHeight)
    $height = [int][Math]::Round($CropHeight * 1024.0 / $image.Width)
    if ($height -gt 285) { throw 'Crop is too tall for an undistorted feature graphic' }
    $destination = [Drawing.Rectangle]::new(0, 500 - $height, 1024, $height)
    $graphics.DrawImage($image, $destination, $source, [Drawing.GraphicsUnit]::Pixel)
    [IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName([IO.Path]::GetFullPath($Output))) | Out-Null
    $bitmap.Save([IO.Path]::GetFullPath($Output), [Drawing.Imaging.ImageFormat]::Png)
} finally {
    $graphics.Dispose()
    $bitmap.Dispose()
    $image.Dispose()
    $title.Dispose()
    $subtitle.Dispose()
    $ink.Dispose()
    $secondary.Dispose()
    $accent.Dispose()
}
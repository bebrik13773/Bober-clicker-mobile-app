/// <summary>
/// Форматирование больших чисел: 1500 → "1.5K", 2000000 → "2M" и т.д.
/// </summary>
public static class NumberFormatter
{
    public static string Format(double value)
    {
        if (value < 1000) return value.ToString("F0");
        if (value < 1_000_000) return (value / 1000.0).ToString("F1") + "K";
        if (value < 1_000_000_000) return (value / 1_000_000.0).ToString("F1") + "M";
        if (value < 1_000_000_000_000) return (value / 1_000_000_000.0).ToString("F1") + "B";
        if (value < 1e15) return (value / 1e12).ToString("F1") + "T";
        if (value < 1e18) return (value / 1e15).ToString("F1") + "Qa";
        return (value / 1e18).ToString("F1") + "Qi";
    }

    public static string FormatPerSec(double value)
    {
        return Format(value) + "/сек";
    }
}

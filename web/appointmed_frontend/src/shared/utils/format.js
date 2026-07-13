/**
 * Shared display formatters used across all three dashboards (admin,
 * doctor, patient) so every appointment time reads the same way everywhere.
 *
 * Times are always transmitted/stored as 24-hour "HH:mm" strings (so
 * sorting and comparisons stay simple); this only formats for display.
 */
export function formatTime12h(time24) {
  if (!time24 || typeof time24 !== "string" || !time24.includes(":")) return time24;

  const [hourStr, minuteStr] = time24.split(":");
  const hour = parseInt(hourStr, 10);
  if (Number.isNaN(hour)) return time24;

  const period = hour >= 12 ? "PM" : "AM";
  const hour12 = hour % 12 === 0 ? 12 : hour % 12;

  return `${hour12}:${minuteStr} ${period}`;
}

/* @ds-bundle: {"format":4,"namespace":"OpenVitalsDesignSystem_626946","components":[{"name":"Button","sourcePath":"components/buttons/Button.jsx"},{"name":"IconButton","sourcePath":"components/buttons/IconButton.jsx"},{"name":"Card","sourcePath":"components/cards/Card.jsx"},{"name":"MetricCard","sourcePath":"components/cards/MetricCard.jsx"},{"name":"MetricStatCard","sourcePath":"components/cards/MetricStatCard.jsx"},{"name":"SummaryRingCard","sourcePath":"components/cards/SummaryRingCard.jsx"},{"name":"MetricBarChart","sourcePath":"components/charts/MetricBarChart.jsx"},{"name":"MetricLineChart","sourcePath":"components/charts/MetricLineChart.jsx"},{"name":"PeriodHeatmap","sourcePath":"components/charts/PeriodHeatmap.jsx"},{"name":"Sparkline","sourcePath":"components/charts/Sparkline.jsx"},{"name":"AccentIconChip","sourcePath":"components/data-display/AccentIconChip.jsx"},{"name":"DetailRow","sourcePath":"components/data-display/DetailRow.jsx"},{"name":"Icon","sourcePath":"components/data-display/Icon.jsx"},{"name":"ReadinessBanner","sourcePath":"components/data-display/ReadinessBanner.jsx"},{"name":"SettingsListItem","sourcePath":"components/data-display/SettingsListItem.jsx"},{"name":"Checkbox","sourcePath":"components/forms/Checkbox.jsx"},{"name":"RadioGroup","sourcePath":"components/forms/RadioGroup.jsx"},{"name":"Select","sourcePath":"components/forms/Select.jsx"},{"name":"Slider","sourcePath":"components/forms/Slider.jsx"},{"name":"Switch","sourcePath":"components/forms/Switch.jsx"},{"name":"TextField","sourcePath":"components/forms/TextField.jsx"},{"name":"AchievementBadge","sourcePath":"components/insights/AchievementBadge.jsx"},{"name":"CrossMetricInsightCard","sourcePath":"components/insights/CrossMetricInsightCard.jsx"},{"name":"DataConfidenceCard","sourcePath":"components/insights/DataConfidenceCard.jsx"},{"name":"SensorStatusCard","sourcePath":"components/insights/SensorStatusCard.jsx"},{"name":"BottomNavBar","sourcePath":"components/navigation/BottomNavBar.jsx"},{"name":"DateNavigator","sourcePath":"components/navigation/DateNavigator.jsx"},{"name":"SectionHeader","sourcePath":"components/navigation/SectionHeader.jsx"},{"name":"TimeRangeSelector","sourcePath":"components/navigation/TimeRangeSelector.jsx"},{"name":"TopBar","sourcePath":"components/navigation/TopBar.jsx"}],"sourceHashes":{"components/buttons/Button.jsx":"bd9eeaa7af92","components/buttons/IconButton.jsx":"dfce2bfbdd54","components/cards/Card.jsx":"2d5d63834770","components/cards/MetricCard.jsx":"24281bbac20c","components/cards/MetricStatCard.jsx":"ea5399c58b5f","components/cards/SummaryRingCard.jsx":"e34a46cc23d4","components/charts/MetricBarChart.jsx":"5de9a70e7c07","components/charts/MetricLineChart.jsx":"9ef7f45638d4","components/charts/PeriodHeatmap.jsx":"e614e130da25","components/charts/Sparkline.jsx":"f915c27403da","components/data-display/AccentIconChip.jsx":"057593d8514c","components/data-display/DetailRow.jsx":"8e06d0b0415a","components/data-display/Icon.jsx":"f2143fed425d","components/data-display/ReadinessBanner.jsx":"8d42f6b7415e","components/data-display/SettingsListItem.jsx":"b67283f08076","components/forms/Checkbox.jsx":"19c3706e93d0","components/forms/RadioGroup.jsx":"16bd2b1850d1","components/forms/Select.jsx":"7c267d5a8c1b","components/forms/Slider.jsx":"5be89bfe4df2","components/forms/Switch.jsx":"15cb5331ab1b","components/forms/TextField.jsx":"1969cb7cc9a7","components/insights/AchievementBadge.jsx":"c76991706f24","components/insights/CrossMetricInsightCard.jsx":"579e10d7469c","components/insights/DataConfidenceCard.jsx":"d928dc048acc","components/insights/SensorStatusCard.jsx":"897f9a19b4a8","components/navigation/BottomNavBar.jsx":"3ef4cf4f166b","components/navigation/DateNavigator.jsx":"693b96569b3d","components/navigation/SectionHeader.jsx":"33a19f146244","components/navigation/TimeRangeSelector.jsx":"29bc0c6fce9b","components/navigation/TopBar.jsx":"5fa1b8a3b724","ui_kits/openvitals-app/AchievementsScreen.jsx":"6ce0ea24bf4a","ui_kits/openvitals-app/ActivityDetailScreen.jsx":"bfeb08d6954c","ui_kits/openvitals-app/BeverageScreen.jsx":"a2ec4662561a","ui_kits/openvitals-app/BodyEnergyScreen.jsx":"64e84df089fd","ui_kits/openvitals-app/DailyReadinessScreen.jsx":"c00273c05488","ui_kits/openvitals-app/DashboardScreen.jsx":"38d84319705b","ui_kits/openvitals-app/DisplaySettingsScreen.jsx":"a31df655db15","ui_kits/openvitals-app/RecordingScreen.jsx":"f18896389028","ui_kits/openvitals-app/SettingsScreen.jsx":"6ff9c50f0832"},"inlinedExternals":[],"unexposedExports":[]} */

(() => {

const __ds_ns = (window.OpenVitalsDesignSystem_626946 = window.OpenVitalsDesignSystem_626946 || {});

const __ds_scope = {};

(__ds_ns.__errors = __ds_ns.__errors || []);

// components/buttons/Button.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/**
 * OpenVitals button — Material 3 button family (Filled / Tonal / Outlined / Text).
 * Mirrors OpenVitalsButton in ui/components/DetailCards.kt. Filled = primary
 * action, Tonal = secondary action (e.g. dashboard "Log"), Outlined/Text = low
 * emphasis. Pill-shaped, 40px min height, label + optional leading icon.
 */
function Button({
  children,
  variant = 'filled',
  size = 'medium',
  icon,
  iconPosition = 'leading',
  disabled = false,
  fullWidth = false,
  onClick,
  style,
  ...rest
}) {
  const heights = {
    small: 36,
    medium: 40,
    large: 48
  };
  const height = heights[size] || 40;
  const palettes = {
    filled: {
      background: 'var(--ov-primary)',
      color: 'var(--ov-on-primary)',
      border: '1px solid transparent'
    },
    tonal: {
      background: 'var(--ov-secondary-container)',
      color: 'var(--ov-on-secondary-container)',
      border: '1px solid transparent'
    },
    outlined: {
      background: 'transparent',
      color: 'var(--ov-primary)',
      border: '1px solid var(--ov-outline-variant)'
    },
    text: {
      background: 'transparent',
      color: 'var(--ov-primary)',
      border: '1px solid transparent'
    }
  };
  const palette = palettes[variant] || palettes.filled;
  const glyph = icon ? /*#__PURE__*/React.createElement("span", {
    className: "material-symbols-outlined",
    style: {
      fontSize: 18,
      lineHeight: 1,
      fontVariationSettings: "'FILL' 0, 'wght' 500, 'opsz' 20"
    },
    "aria-hidden": "true"
  }, icon) : null;
  return /*#__PURE__*/React.createElement("button", _extends({
    type: "button",
    onClick: disabled ? undefined : onClick,
    disabled: disabled,
    style: {
      display: 'inline-flex',
      alignItems: 'center',
      justifyContent: 'center',
      gap: 8,
      height,
      width: fullWidth ? '100%' : 'auto',
      padding: `0 ${size === 'small' ? 16 : 24}px`,
      borderRadius: 'var(--ov-radius-full)',
      fontFamily: 'var(--ov-font-sans)',
      fontSize: 'var(--ov-label-lg-size)',
      fontWeight: 'var(--ov-weight-medium)',
      letterSpacing: 'var(--ov-label-lg-tracking)',
      cursor: disabled ? 'not-allowed' : 'pointer',
      opacity: disabled ? 0.38 : 1,
      transition: 'filter 120ms ease, opacity 120ms ease',
      WebkitTapHighlightColor: 'transparent',
      ...palette,
      ...style
    },
    onMouseDown: e => {
      if (!disabled) e.currentTarget.style.filter = 'brightness(0.94)';
    },
    onMouseUp: e => {
      e.currentTarget.style.filter = 'none';
    },
    onMouseLeave: e => {
      e.currentTarget.style.filter = 'none';
    }
  }, rest), glyph && iconPosition === 'leading' && glyph, /*#__PURE__*/React.createElement("span", null, children), glyph && iconPosition === 'trailing' && glyph);
}
Object.assign(__ds_scope, { Button });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/buttons/Button.jsx", error: String((e && e.message) || e) }); }

// components/buttons/IconButton.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/**
 * Icon-only button. Two treatments from source:
 *  - "plain"   → bare IconButton (44px touch target), for top-bar actions.
 *  - "surface" → OpenVitalsIconSurfaceButton: 52px filled circle on a
 *    surface-container tone, used for the date-nav chevrons / calendar.
 */
function IconButton({
  icon,
  variant = 'plain',
  size,
  disabled = false,
  label,
  onClick,
  style,
  ...rest
}) {
  const isSurface = variant === 'surface';
  const dim = size || (isSurface ? 52 : 44);
  const glyphSize = isSurface ? 24 : 22;
  return /*#__PURE__*/React.createElement("button", _extends({
    type: "button",
    "aria-label": label,
    title: label,
    onClick: disabled ? undefined : onClick,
    disabled: disabled,
    style: {
      display: 'inline-flex',
      alignItems: 'center',
      justifyContent: 'center',
      width: dim,
      height: dim,
      borderRadius: 'var(--ov-radius-full)',
      border: 'none',
      background: isSurface ? 'var(--ov-surface-container)' : 'transparent',
      color: isSurface ? 'var(--ov-on-surface)' : 'var(--ov-on-surface-variant)',
      cursor: disabled ? 'not-allowed' : 'pointer',
      opacity: disabled ? 0.38 : 1,
      transition: 'filter 120ms ease, background 120ms ease',
      WebkitTapHighlightColor: 'transparent',
      ...style
    },
    onMouseDown: e => {
      if (!disabled) e.currentTarget.style.filter = 'brightness(0.93)';
    },
    onMouseUp: e => {
      e.currentTarget.style.filter = 'none';
    },
    onMouseLeave: e => {
      e.currentTarget.style.filter = 'none';
    }
  }, rest), /*#__PURE__*/React.createElement("span", {
    className: "material-symbols-outlined",
    style: {
      fontSize: glyphSize,
      lineHeight: 1,
      fontVariationSettings: "'FILL' 0, 'wght' 500, 'opsz' 24"
    },
    "aria-hidden": "true"
  }, icon));
}
Object.assign(__ds_scope, { IconButton });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/buttons/IconButton.jsx", error: String((e && e.message) || e) }); }

// components/cards/Card.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/**
 * OpenVitalsCard — the base surface for every dashboard/detail card.
 * Flat (0 elevation), 16px (medium) radius, sits on a surface-container tone.
 * Depth comes from tone steps, not shadow. Mirrors OpenVitalsCard in
 * ui/components/DetailCards.kt with its style variants.
 */
function Card({
  children,
  variant = 'neutral',
  accentColor,
  radius = 'md',
  onClick,
  padding,
  style,
  ...rest
}) {
  const radii = {
    xs: 'var(--ov-radius-xs)',
    sm: 'var(--ov-radius-sm)',
    md: 'var(--ov-radius-md)',
    lg: 'var(--ov-radius-lg)',
    xl: 'var(--ov-radius-xl)'
  };
  let background;
  switch (variant) {
    case 'metric':
      background = 'var(--ov-surface-container-highest)';
      break;
    case 'accent':
      background = accentColor ? `color-mix(in srgb, ${accentColor} 9%, var(--ov-surface-container))` : 'var(--ov-surface-container)';
      break;
    case 'error':
      background = 'var(--ov-error-container)';
      break;
    default:
      background = 'var(--ov-surface-container)';
  }
  const interactive = typeof onClick === 'function';
  return /*#__PURE__*/React.createElement("div", _extends({
    role: interactive ? 'button' : undefined,
    tabIndex: interactive ? 0 : undefined,
    onClick: onClick,
    style: {
      background,
      borderRadius: radii[radius] || radii.md,
      padding: padding != null ? padding : undefined,
      color: variant === 'error' ? 'var(--ov-on-error-container)' : 'var(--ov-on-surface)',
      cursor: interactive ? 'pointer' : 'default',
      transition: 'filter 120ms ease',
      boxSizing: 'border-box',
      ...style
    },
    onMouseEnter: interactive ? e => {
      e.currentTarget.style.filter = 'brightness(0.98)';
    } : undefined,
    onMouseLeave: interactive ? e => {
      e.currentTarget.style.filter = 'none';
    } : undefined
  }, rest), children);
}
Object.assign(__ds_scope, { Card });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/cards/Card.jsx", error: String((e && e.message) || e) }); }

// components/cards/MetricCard.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/**
 * MetricCard — the larger detail metric surface (icon + title header, big value
 * with unit, optional subtitle and source chip). From ui/components/MetricCard.kt.
 * Used on detail screens and as a full-width dashboard tile.
 */
function MetricCard({
  title,
  value,
  unit,
  icon,
  accentColor = 'var(--ov-primary)',
  subtitle,
  source,
  onClick,
  style,
  ...rest
}) {
  const interactive = typeof onClick === 'function';
  return /*#__PURE__*/React.createElement("div", _extends({
    role: interactive ? 'button' : undefined,
    onClick: onClick,
    style: {
      background: 'var(--ov-surface-container)',
      borderRadius: 'var(--ov-radius-md)',
      padding: 16,
      cursor: interactive ? 'pointer' : 'default',
      boxSizing: 'border-box',
      ...style
    }
  }, rest), /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      alignItems: 'center',
      gap: 8
    }
  }, /*#__PURE__*/React.createElement("span", {
    className: "material-symbols-outlined",
    style: {
      fontSize: 20,
      color: accentColor,
      fontVariationSettings: "'FILL' 0, 'wght' 500, 'opsz' 20"
    },
    "aria-hidden": "true"
  }, icon), /*#__PURE__*/React.createElement("span", {
    style: {
      font: 'var(--ov-weight-medium) var(--ov-label-md-size)/var(--ov-label-md-line) var(--ov-font-sans)',
      color: 'var(--ov-on-surface-variant)'
    }
  }, title)), /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      alignItems: 'flex-end',
      gap: 4,
      marginTop: 12
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      font: 'var(--ov-weight-bold) var(--ov-headline-sm-size)/var(--ov-headline-sm-line) var(--ov-font-sans)',
      color: 'var(--ov-on-surface)',
      fontFeatureSettings: "'tnum'"
    }
  }, value), unit ? /*#__PURE__*/React.createElement("span", {
    style: {
      font: 'var(--ov-weight-regular) var(--ov-body-sm-size)/var(--ov-body-sm-line) var(--ov-font-sans)',
      color: 'var(--ov-on-surface-variant)',
      paddingBottom: 3
    }
  }, unit) : null), subtitle ? /*#__PURE__*/React.createElement("div", {
    style: {
      font: 'var(--ov-weight-regular) var(--ov-body-sm-size)/var(--ov-body-sm-line) var(--ov-font-sans)',
      color: 'var(--ov-on-surface-variant)',
      marginTop: 4
    }
  }, subtitle) : null, source ? /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'inline-flex',
      alignItems: 'center',
      gap: 6,
      marginTop: 8,
      padding: '3px 8px',
      borderRadius: 'var(--ov-radius-full)',
      background: 'var(--ov-surface-container-highest)'
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      width: 12,
      height: 12,
      borderRadius: '50%',
      background: 'var(--ov-outline)'
    }
  }), /*#__PURE__*/React.createElement("span", {
    style: {
      font: 'var(--ov-weight-medium) var(--ov-label-sm-size)/var(--ov-label-sm-line) var(--ov-font-sans)',
      color: 'var(--ov-on-surface-variant)'
    }
  }, source)) : null);
}
Object.assign(__ds_scope, { MetricCard });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/cards/MetricCard.jsx", error: String((e && e.message) || e) }); }

// components/cards/MetricStatCard.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/**
 * MetricStatCard — the small dashboard stat tile (Distance, Calories, Sleep…).
 * Layout from features/dashboard/components/MetricStatCard.kt:
 *  - 28px accent-tinted icon circle (16px glyph)
 *  - title (labelMedium, on-surface-variant) + value (titleMedium, semibold)
 *  - optional 3px accent progress underline pinned to the bottom edge.
 */
function MetricStatCard({
  title,
  value,
  unit,
  icon,
  accentColor = 'var(--ov-primary)',
  subtitle,
  progress,
  onClick,
  style,
  ...rest
}) {
  const interactive = typeof onClick === 'function';
  return /*#__PURE__*/React.createElement("div", _extends({
    role: interactive ? 'button' : undefined,
    onClick: onClick,
    style: {
      position: 'relative',
      overflow: 'hidden',
      background: 'var(--ov-surface-container)',
      borderRadius: 'var(--ov-radius-md)',
      padding: '10px 12px',
      display: 'flex',
      alignItems: 'center',
      gap: 10,
      cursor: interactive ? 'pointer' : 'default',
      boxSizing: 'border-box',
      ...style
    }
  }, rest), /*#__PURE__*/React.createElement("div", {
    style: {
      flex: '0 0 auto',
      width: 28,
      height: 28,
      borderRadius: 'var(--ov-radius-full)',
      background: `color-mix(in srgb, ${accentColor} 16%, var(--ov-surface-container-highest))`,
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center'
    }
  }, /*#__PURE__*/React.createElement("span", {
    className: "material-symbols-outlined",
    style: {
      fontSize: 16,
      color: accentColor,
      fontVariationSettings: "'FILL' 0, 'wght' 500, 'opsz' 20"
    },
    "aria-hidden": "true"
  }, icon)), /*#__PURE__*/React.createElement("div", {
    style: {
      minWidth: 0,
      flex: 1
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: 'var(--ov-weight-medium) var(--ov-label-md-size)/var(--ov-label-md-line) var(--ov-font-sans)',
      color: 'var(--ov-on-surface-variant)',
      whiteSpace: 'nowrap',
      overflow: 'hidden',
      textOverflow: 'ellipsis'
    }
  }, title), /*#__PURE__*/React.createElement("div", {
    style: {
      font: 'var(--ov-weight-semibold) var(--ov-title-md-size)/var(--ov-title-md-line) var(--ov-font-sans)',
      color: 'var(--ov-on-surface)',
      whiteSpace: 'nowrap',
      overflow: 'hidden',
      textOverflow: 'ellipsis'
    }
  }, value, unit ? /*#__PURE__*/React.createElement("span", {
    style: {
      fontWeight: 'var(--ov-weight-medium)'
    }
  }, " ", unit) : null), subtitle ? /*#__PURE__*/React.createElement("div", {
    style: {
      font: 'var(--ov-weight-medium) var(--ov-label-sm-size)/var(--ov-label-sm-line) var(--ov-font-sans)',
      color: 'var(--ov-on-surface-variant)',
      whiteSpace: 'nowrap',
      overflow: 'hidden',
      textOverflow: 'ellipsis'
    }
  }, subtitle) : null), progress != null ? /*#__PURE__*/React.createElement("div", {
    style: {
      position: 'absolute',
      left: 0,
      bottom: 0,
      height: 3,
      borderRadius: 'var(--ov-radius-xs)',
      width: `${Math.max(0, Math.min(1, progress)) * 100}%`,
      background: `color-mix(in srgb, ${accentColor} 55%, transparent)`
    }
  }) : null);
}
Object.assign(__ds_scope, { MetricStatCard });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/cards/MetricStatCard.jsx", error: String((e && e.message) || e) }); }

// components/cards/SummaryRingCard.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/**
 * DashboardSummaryCard — the large hero stat with an open progress ring
 * (Steps, Weekly cardio). From features/dashboard/components/DashboardSummaryCard.kt:
 * a 280°-sweep arc starting at 130° (open at the bottom), round caps; the fill
 * is the accent at ~65% alpha over an outline-variant track. Title / value /
 * subtitle are stacked in the centre.
 */
function polar(cx, cy, r, deg) {
  const rad = deg * Math.PI / 180;
  return {
    x: cx + r * Math.cos(rad),
    y: cy + r * Math.sin(rad)
  };
}
function arcPath(cx, cy, r, startDeg, sweepDeg) {
  const end = startDeg + sweepDeg;
  const s = polar(cx, cy, r, startDeg);
  const e = polar(cx, cy, r, end);
  const largeArc = sweepDeg > 180 ? 1 : 0;
  return `M ${s.x} ${s.y} A ${r} ${r} 0 ${largeArc} 1 ${e.x} ${e.y}`;
}
function SummaryRingCard({
  title,
  value,
  subtitle,
  progress = 0,
  accentColor = 'var(--ov-metric-steps)',
  size = 168,
  onClick,
  style,
  ...rest
}) {
  const START = 130;
  const SWEEP = 280;
  const stroke = Math.max(5, Math.min(10, size * 0.09));
  const r = (size - stroke) / 2 - 2;
  const cx = size / 2;
  const cy = size / 2;
  const frac = Math.max(0, Math.min(1, progress));
  const interactive = typeof onClick === 'function';
  return /*#__PURE__*/React.createElement("div", _extends({
    role: interactive ? 'button' : undefined,
    onClick: onClick,
    style: {
      background: 'var(--ov-surface-container)',
      borderRadius: 'var(--ov-radius-md)',
      padding: 6,
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      cursor: interactive ? 'pointer' : 'default',
      boxSizing: 'border-box',
      ...style
    }
  }, rest), /*#__PURE__*/React.createElement("div", {
    style: {
      position: 'relative',
      width: size,
      height: size
    }
  }, /*#__PURE__*/React.createElement("svg", {
    width: size,
    height: size,
    style: {
      display: 'block'
    }
  }, /*#__PURE__*/React.createElement("path", {
    d: arcPath(cx, cy, r, START, SWEEP),
    fill: "none",
    stroke: "var(--ov-outline-variant)",
    strokeWidth: stroke,
    strokeLinecap: "round"
  }), /*#__PURE__*/React.createElement("path", {
    d: arcPath(cx, cy, r, START, SWEEP * frac),
    fill: "none",
    stroke: accentColor,
    strokeWidth: stroke,
    strokeLinecap: "round",
    style: {
      opacity: 0.72
    }
  })), /*#__PURE__*/React.createElement("div", {
    style: {
      position: 'absolute',
      inset: 0,
      display: 'flex',
      flexDirection: 'column',
      alignItems: 'center',
      justifyContent: 'center',
      textAlign: 'center',
      padding: stroke + 6
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: 'var(--ov-weight-medium) var(--ov-label-sm-size)/var(--ov-label-sm-line) var(--ov-font-sans)',
      color: 'var(--ov-on-surface-variant)'
    }
  }, title), /*#__PURE__*/React.createElement("div", {
    style: {
      font: 'var(--ov-weight-bold) var(--ov-headline-sm-size)/var(--ov-headline-sm-line) var(--ov-font-sans)',
      color: 'var(--ov-on-surface)',
      fontFeatureSettings: "'tnum'"
    }
  }, value), subtitle ? /*#__PURE__*/React.createElement("div", {
    style: {
      font: 'var(--ov-weight-medium) var(--ov-label-sm-size)/var(--ov-label-sm-line) var(--ov-font-sans)',
      color: 'var(--ov-on-surface-variant)'
    }
  }, subtitle) : null)));
}
Object.assign(__ds_scope, { SummaryRingCard });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/cards/SummaryRingCard.jsx", error: String((e && e.message) || e) }); }

// components/charts/MetricBarChart.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/**
 * MetricBarChart — rounded accent bars with optional X labels, for period
 * summaries (weekly steps, daily calories). Bars use the accent at ~0.85 alpha;
 * a highlighted index renders at full accent. Matches PeriodChart bar styling.
 */
function MetricBarChart({
  data = [],
  labels = [],
  accentColor = 'var(--ov-metric-steps)',
  height = 160,
  highlightIndex,
  max,
  style,
  ...rest
}) {
  const vals = data.length ? data : [0];
  const hi = max != null ? max : Math.max(...vals, 1);
  const gap = 6;
  return /*#__PURE__*/React.createElement("div", _extends({
    style: {
      ...style
    }
  }, rest), /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      alignItems: 'flex-end',
      gap,
      height
    }
  }, vals.map((v, i) => {
    const h = Math.max(2, v / hi * (height - 4));
    const active = highlightIndex == null ? true : i === highlightIndex;
    return /*#__PURE__*/React.createElement("div", {
      key: i,
      title: String(v),
      style: {
        flex: 1,
        height: h,
        borderRadius: 'var(--ov-radius-xs)',
        background: accentColor,
        opacity: active ? 0.9 : 0.35,
        transition: 'opacity 120ms ease'
      }
    });
  })), labels.length ? /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      gap,
      marginTop: 6
    }
  }, labels.map((l, i) => /*#__PURE__*/React.createElement("div", {
    key: i,
    style: {
      flex: 1,
      textAlign: 'center',
      font: 'var(--ov-weight-medium) var(--ov-label-sm-size)/var(--ov-label-sm-line) var(--ov-font-sans)',
      color: 'var(--ov-on-surface-variant)'
    }
  }, l))) : null);
}
Object.assign(__ds_scope, { MetricBarChart });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/charts/MetricBarChart.jsx", error: String((e && e.message) || e) }); }

// components/charts/MetricLineChart.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/**
 * MetricLineChart — a smooth accent line over a soft area fill with a faint
 * baseline/gridline, matching the detail-screen trend charts (Body Energy
 * timeline, heart-rate over time). Renders an SVG with an accent stroke, an
 * accent gradient fill, and optional Y grid lines + labels.
 */
function MetricLineChart({
  data = [],
  accentColor = 'var(--ov-metric-workout)',
  height = 180,
  min,
  max,
  yTicks = [0, 50, 100],
  showArea = true,
  showDots = false,
  style,
  ...rest
}) {
  const W = 320;
  const H = height;
  const padL = 34,
    padR = 8,
    padT = 10,
    padB = 8;
  const vals = data.length ? data : [0];
  const lo = min != null ? min : Math.min(...vals, ...(yTicks.length ? yTicks : []));
  const hi = max != null ? max : Math.max(...vals, ...(yTicks.length ? yTicks : []));
  const span = hi - lo || 1;
  const gid = React.useMemo(() => 'ovlg' + Math.random().toString(36).slice(2, 8), []);
  const x = i => padL + i / Math.max(1, vals.length - 1) * (W - padL - padR);
  const y = v => padT + (1 - (v - lo) / span) * (H - padT - padB);
  const pts = vals.map((v, i) => [x(i), y(v)]);
  // smooth path (catmull-rom → bezier)
  let d = '';
  if (pts.length) {
    d = `M ${pts[0][0]} ${pts[0][1]}`;
    for (let i = 0; i < pts.length - 1; i++) {
      const p0 = pts[i - 1] || pts[i];
      const p1 = pts[i];
      const p2 = pts[i + 1];
      const p3 = pts[i + 2] || p2;
      const c1x = p1[0] + (p2[0] - p0[0]) / 6;
      const c1y = p1[1] + (p2[1] - p0[1]) / 6;
      const c2x = p2[0] - (p3[0] - p1[0]) / 6;
      const c2y = p2[1] - (p3[1] - p1[1]) / 6;
      d += ` C ${c1x} ${c1y}, ${c2x} ${c2y}, ${p2[0]} ${p2[1]}`;
    }
  }
  const area = d ? `${d} L ${x(vals.length - 1)} ${H - padB} L ${x(0)} ${H - padB} Z` : '';
  return /*#__PURE__*/React.createElement("svg", _extends({
    viewBox: `0 0 ${W} ${H}`,
    width: "100%",
    height: H,
    preserveAspectRatio: "none",
    style: {
      display: 'block',
      ...style
    }
  }, rest), /*#__PURE__*/React.createElement("defs", null, /*#__PURE__*/React.createElement("linearGradient", {
    id: gid,
    x1: "0",
    y1: "0",
    x2: "0",
    y2: "1"
  }, /*#__PURE__*/React.createElement("stop", {
    offset: "0%",
    stopColor: accentColor,
    stopOpacity: "0.28"
  }), /*#__PURE__*/React.createElement("stop", {
    offset: "100%",
    stopColor: accentColor,
    stopOpacity: "0"
  }))), yTicks.map((t, i) => /*#__PURE__*/React.createElement("g", {
    key: i
  }, /*#__PURE__*/React.createElement("line", {
    x1: padL,
    y1: y(t),
    x2: W - padR,
    y2: y(t),
    stroke: "var(--ov-outline-variant)",
    strokeWidth: "1",
    opacity: "0.5"
  }), /*#__PURE__*/React.createElement("text", {
    x: padL - 6,
    y: y(t) + 3,
    textAnchor: "end",
    fontFamily: "var(--ov-font-sans)",
    fontSize: "10",
    fontWeight: "500",
    fill: "var(--ov-on-surface-variant)"
  }, t))), showArea && area ? /*#__PURE__*/React.createElement("path", {
    d: area,
    fill: `url(#${gid})`
  }) : null, d ? /*#__PURE__*/React.createElement("path", {
    d: d,
    fill: "none",
    stroke: accentColor,
    strokeWidth: "2.5",
    strokeLinecap: "round",
    strokeLinejoin: "round"
  }) : null, showDots ? pts.map(([px, py], i) => /*#__PURE__*/React.createElement("circle", {
    key: i,
    cx: px,
    cy: py,
    r: "3",
    fill: accentColor
  })) : null);
}
Object.assign(__ds_scope, { MetricLineChart });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/charts/MetricLineChart.jsx", error: String((e && e.message) || e) }); }

// components/charts/PeriodHeatmap.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/**
 * PeriodHeatmap — the month calendar heatmap (ui/charts/PeriodHeatmap.kt ·
 * PeriodMonthHeatmap). A weekday header, a Mon-start grid of square cells
 * shaded by value (accent alpha 0.25→1.0), empty cells before the 1st, day
 * numbers inside, and a Less→More legend. Zero-value days use a faint
 * surface tone.
 */
function PeriodHeatmap({
  title,
  summary,
  values = [],
  // one number per day of the month (index 0 = day 1)
  startWeekday = 0,
  // 0 = Monday … 6 = Sunday (leading empty cells)
  accentColor = 'var(--ov-metric-steps)',
  style,
  ...rest
}) {
  const weekdays = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];
  const positives = values.filter(v => v > 0);
  const minPos = positives.length ? Math.min(...positives) : 0;
  const maxVal = Math.max(1, ...values);
  const cellColor = v => {
    if (v == null) return 'transparent';
    if (v <= 0) return 'color-mix(in srgb, var(--ov-surface-container-highest) 65%, transparent)';
    const frac = maxVal <= minPos ? 1 : Math.max(0, Math.min(1, (v - minPos) / (maxVal - minPos)));
    return `color-mix(in srgb, ${accentColor} ${Math.round((0.25 + 0.75 * frac) * 100)}%, transparent)`;
  };
  const cells = [];
  for (let i = 0; i < startWeekday; i++) cells.push({
    day: null,
    value: null
  });
  values.forEach((v, i) => cells.push({
    day: i + 1,
    value: v
  }));
  while (cells.length % 7 !== 0) cells.push({
    day: null,
    value: null
  });
  const rows = [];
  for (let i = 0; i < cells.length; i += 7) rows.push(cells.slice(i, i + 7));
  const legend = [0, 1, 2, 3, 4].map(i => maxVal <= minPos ? maxVal : minPos + (maxVal - minPos) * i / 4);
  return /*#__PURE__*/React.createElement("div", _extends({
    style: {
      background: 'var(--ov-surface-container)',
      borderRadius: 'var(--ov-radius-md)',
      padding: 16,
      ...style
    }
  }, rest), title ? /*#__PURE__*/React.createElement("div", {
    style: {
      font: 'var(--ov-weight-medium) var(--ov-title-sm-size)/var(--ov-title-sm-line) var(--ov-font-sans)',
      color: 'var(--ov-on-surface)'
    }
  }, title) : null, summary ? /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 4,
      font: 'var(--ov-weight-regular) var(--ov-body-sm-size)/var(--ov-body-sm-line) var(--ov-font-sans)',
      color: 'var(--ov-on-surface-variant)'
    }
  }, summary) : null, /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'grid',
      gridTemplateColumns: 'repeat(7, 1fr)',
      gap: 8,
      marginTop: 16
    }
  }, weekdays.map(w => /*#__PURE__*/React.createElement("div", {
    key: w,
    style: {
      textAlign: 'center',
      font: 'var(--ov-weight-medium) var(--ov-label-sm-size)/var(--ov-label-sm-line) var(--ov-font-sans)',
      color: 'var(--ov-on-surface-variant)'
    }
  }, w)), rows.flat().map((c, i) => /*#__PURE__*/React.createElement("div", {
    key: i,
    style: {
      aspectRatio: '1 / 1',
      borderRadius: 'var(--ov-radius-sm)',
      background: cellColor(c.value),
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center'
    }
  }, c.day != null ? /*#__PURE__*/React.createElement("span", {
    style: {
      font: 'var(--ov-weight-medium) var(--ov-label-sm-size)/1 var(--ov-font-sans)',
      color: 'var(--ov-on-surface)'
    }
  }, c.day) : null))), /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      alignItems: 'center',
      gap: 6,
      marginTop: 12
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      font: 'var(--ov-weight-medium) var(--ov-label-sm-size)/1 var(--ov-font-sans)',
      color: 'var(--ov-on-surface-variant)'
    }
  }, "Less"), /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1
    }
  }), legend.map((v, i) => /*#__PURE__*/React.createElement("span", {
    key: i,
    style: {
      width: 12,
      height: 12,
      borderRadius: 999,
      background: cellColor(Math.max(v, 0.0001))
    }
  })), /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1
    }
  }), /*#__PURE__*/React.createElement("span", {
    style: {
      font: 'var(--ov-weight-medium) var(--ov-label-sm-size)/1 var(--ov-font-sans)',
      color: 'var(--ov-on-surface-variant)'
    }
  }, "More")));
}
Object.assign(__ds_scope, { PeriodHeatmap });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/charts/PeriodHeatmap.jsx", error: String((e && e.message) || e) }); }

// components/charts/Sparkline.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/**
 * Sparkline — a tiny inline accent trend line (no axes/labels), for embedding
 * in metric cards and rows. Mirrors MetricSparklineChart.
 */
function Sparkline({
  data = [],
  accentColor = 'var(--ov-primary)',
  width = 96,
  height = 28,
  strokeWidth = 2,
  style,
  ...rest
}) {
  const vals = data.length ? data : [0, 0];
  const lo = Math.min(...vals);
  const hi = Math.max(...vals);
  const span = hi - lo || 1;
  const pad = strokeWidth;
  const x = i => pad + i / Math.max(1, vals.length - 1) * (width - pad * 2);
  const y = v => pad + (1 - (v - lo) / span) * (height - pad * 2);
  const d = vals.map((v, i) => `${i ? 'L' : 'M'} ${x(i).toFixed(1)} ${y(v).toFixed(1)}`).join(' ');
  return /*#__PURE__*/React.createElement("svg", _extends({
    width: width,
    height: height,
    viewBox: `0 0 ${width} ${height}`,
    style: {
      display: 'block',
      ...style
    }
  }, rest), /*#__PURE__*/React.createElement("path", {
    d: d,
    fill: "none",
    stroke: accentColor,
    strokeWidth: strokeWidth,
    strokeLinecap: "round",
    strokeLinejoin: "round"
  }));
}
Object.assign(__ds_scope, { Sparkline });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/charts/Sparkline.jsx", error: String((e && e.message) || e) }); }

// components/data-display/AccentIconChip.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/**
 * AccentIconChip — a small round accent-tinted icon badge (AccentIconChip in
 * DetailCards.kt). Circular, accent at 14% alpha, colored glyph. Used as the
 * leading marker in insight rows, readiness banner, drink rows, etc.
 */
function AccentIconChip({
  icon,
  color = 'var(--ov-primary)',
  size = 40,
  iconSize,
  style,
  ...rest
}) {
  const glyph = iconSize || Math.round(size * 0.5);
  return /*#__PURE__*/React.createElement("div", _extends({
    style: {
      width: size,
      height: size,
      borderRadius: 'var(--ov-radius-full)',
      background: `color-mix(in srgb, ${color} 14%, transparent)`,
      display: 'inline-flex',
      alignItems: 'center',
      justifyContent: 'center',
      flex: '0 0 auto',
      ...style
    }
  }, rest), /*#__PURE__*/React.createElement("span", {
    className: "material-symbols-outlined",
    style: {
      fontSize: glyph,
      color,
      fontVariationSettings: "'FILL' 0, 'wght' 500, 'opsz' 24"
    },
    "aria-hidden": "true"
  }, icon));
}
Object.assign(__ds_scope, { AccentIconChip });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/data-display/AccentIconChip.jsx", error: String((e && e.message) || e) }); }

// components/data-display/DetailRow.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/**
 * DetailRow — a label/value row (DetailRow in DetailCards.kt). Label on the
 * left in on-surface-variant, value right-aligned. The building block of the
 * activity-detail "Metrics" list.
 */
function DetailRow({
  label,
  value,
  style,
  ...rest
}) {
  return /*#__PURE__*/React.createElement("div", _extends({
    style: {
      display: 'flex',
      alignItems: 'flex-start',
      justifyContent: 'space-between',
      gap: 16,
      padding: '2px 0',
      ...style
    }
  }, rest), /*#__PURE__*/React.createElement("span", {
    style: {
      flex: '0 1 42%',
      font: 'var(--ov-weight-regular) var(--ov-body-md-size)/var(--ov-body-md-line) var(--ov-font-sans)',
      color: 'var(--ov-on-surface-variant)'
    }
  }, label), /*#__PURE__*/React.createElement("span", {
    style: {
      flex: '1 1 58%',
      textAlign: 'right',
      font: 'var(--ov-weight-regular) var(--ov-body-md-size)/var(--ov-body-md-line) var(--ov-font-sans)',
      color: 'var(--ov-on-surface)'
    }
  }, value));
}
Object.assign(__ds_scope, { DetailRow });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/data-display/DetailRow.jsx", error: String((e && e.message) || e) }); }

// components/data-display/Icon.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/**
 * Icon — thin wrapper over Material Symbols Outlined (the app's icon set,
 * androidx.compose.material.icons). Use everywhere a glyph is needed.
 */
function Icon({
  name,
  size = 24,
  color = 'currentColor',
  weight = 500,
  fill = 0,
  style,
  ...rest
}) {
  return /*#__PURE__*/React.createElement("span", _extends({
    className: "material-symbols-outlined",
    "aria-hidden": "true",
    style: {
      fontSize: size,
      lineHeight: 1,
      color,
      fontVariationSettings: `'FILL' ${fill}, 'wght' ${weight}, 'opsz' ${Math.min(48, Math.max(20, size))}`,
      ...style
    }
  }, rest), name);
}
Object.assign(__ds_scope, { Icon });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/data-display/Icon.jsx", error: String((e && e.message) || e) }); }

// components/data-display/ReadinessBanner.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/**
 * ReadinessBanner — the hero card on the Daily Readiness screen. An accent
 * (heart-pink) tinted card: leading chip + title + confidence line, a large
 * score at the top-right, an accent headline, and a body explanation. Children
 * render below the body (e.g. the Body Energy / Training Readiness sub-tiles).
 */
function ReadinessBanner({
  icon = 'self_improvement',
  title = 'Daily Readiness',
  confidence,
  score,
  scoreLabel = 'Readiness',
  headline,
  body,
  accentColor = 'var(--ov-metric-heart)',
  children,
  style,
  ...rest
}) {
  return /*#__PURE__*/React.createElement("div", _extends({
    style: {
      background: `color-mix(in srgb, ${accentColor} 9%, var(--ov-surface-container))`,
      borderRadius: 'var(--ov-radius-md)',
      padding: 20,
      ...style
    }
  }, rest), /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      alignItems: 'flex-start',
      gap: 12
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      width: 48,
      height: 48,
      borderRadius: 'var(--ov-radius-full)',
      flex: '0 0 auto',
      background: `color-mix(in srgb, ${accentColor} 16%, transparent)`,
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center'
    }
  }, /*#__PURE__*/React.createElement("span", {
    className: "material-symbols-outlined",
    style: {
      fontSize: 26,
      color: accentColor
    },
    "aria-hidden": "true"
  }, icon)), /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1,
      minWidth: 0
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: 'var(--ov-weight-semibold) var(--ov-title-md-size)/var(--ov-title-md-line) var(--ov-font-sans)',
      color: 'var(--ov-on-surface)'
    }
  }, title), confidence ? /*#__PURE__*/React.createElement("div", {
    style: {
      font: 'var(--ov-weight-regular) var(--ov-body-sm-size)/var(--ov-body-sm-line) var(--ov-font-sans)',
      color: 'var(--ov-on-surface-variant)'
    }
  }, confidence) : null), score != null ? /*#__PURE__*/React.createElement("div", {
    style: {
      textAlign: 'right',
      flex: '0 0 auto'
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: 'var(--ov-weight-medium) var(--ov-label-sm-size)/var(--ov-label-sm-line) var(--ov-font-sans)',
      color: 'var(--ov-on-surface-variant)'
    }
  }, scoreLabel), /*#__PURE__*/React.createElement("div", {
    style: {
      font: 'var(--ov-weight-bold) var(--ov-headline-md-size)/var(--ov-headline-md-line) var(--ov-font-sans)',
      color: accentColor,
      fontFeatureSettings: "'tnum'"
    }
  }, score)) : null), headline ? /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 14,
      font: 'var(--ov-weight-bold) var(--ov-headline-sm-size)/var(--ov-headline-sm-line) var(--ov-font-sans)',
      color: accentColor
    }
  }, headline) : null, body ? /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 8,
      font: 'var(--ov-weight-regular) var(--ov-body-lg-size)/var(--ov-body-lg-line) var(--ov-font-sans)',
      color: 'var(--ov-on-surface)'
    }
  }, body) : null, children ? /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 16
    }
  }, children) : null);
}
Object.assign(__ds_scope, { ReadinessBanner });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/data-display/ReadinessBanner.jsx", error: String((e && e.message) || e) }); }

// components/data-display/SettingsListItem.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/**
 * SettingsListItem — a tappable settings row on a card surface: leading glyph,
 * title + supporting text, trailing chevron. Matches the Settings screen rows
 * (Display, Activities, Sensors & devices…). Each row is its own card.
 */
function SettingsListItem({
  icon,
  title,
  supportingText,
  onClick,
  trailing = 'chevron_right',
  style,
  ...rest
}) {
  return /*#__PURE__*/React.createElement("div", _extends({
    role: "button",
    onClick: onClick,
    style: {
      display: 'flex',
      alignItems: 'center',
      gap: 16,
      background: 'var(--ov-surface-container)',
      borderRadius: 'var(--ov-radius-md)',
      padding: '16px 16px',
      cursor: onClick ? 'pointer' : 'default',
      boxSizing: 'border-box',
      transition: 'filter 120ms ease',
      ...style
    },
    onMouseEnter: e => {
      if (onClick) e.currentTarget.style.filter = 'brightness(0.98)';
    },
    onMouseLeave: e => {
      e.currentTarget.style.filter = 'none';
    }
  }, rest), icon ? /*#__PURE__*/React.createElement("span", {
    className: "material-symbols-outlined",
    style: {
      fontSize: 24,
      color: 'var(--ov-on-surface)',
      flex: '0 0 auto',
      fontVariationSettings: "'FILL' 0, 'wght' 500, 'opsz' 24"
    },
    "aria-hidden": "true"
  }, icon) : null, /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1,
      minWidth: 0
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: 'var(--ov-weight-semibold) var(--ov-title-md-size)/var(--ov-title-md-line) var(--ov-font-sans)',
      color: 'var(--ov-on-surface)'
    }
  }, title), supportingText ? /*#__PURE__*/React.createElement("div", {
    style: {
      font: 'var(--ov-weight-regular) var(--ov-body-md-size)/var(--ov-body-md-line) var(--ov-font-sans)',
      color: 'var(--ov-on-surface-variant)',
      marginTop: 2
    }
  }, supportingText) : null), trailing ? /*#__PURE__*/React.createElement("span", {
    className: "material-symbols-outlined",
    style: {
      fontSize: 22,
      color: 'var(--ov-on-surface-variant)',
      flex: '0 0 auto'
    },
    "aria-hidden": "true"
  }, trailing) : null);
}
Object.assign(__ds_scope, { SettingsListItem });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/data-display/SettingsListItem.jsx", error: String((e && e.message) || e) }); }

// components/forms/Checkbox.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/**
 * Checkbox — Material 3 checkbox. Unchecked: 2px outline square. Checked:
 * primary fill with an on-primary check glyph. Optional trailing label.
 */
function Checkbox({
  checked = false,
  onChange,
  disabled = false,
  label,
  style,
  ...rest
}) {
  const toggle = () => {
    if (!disabled && onChange) onChange(!checked);
  };
  const box = /*#__PURE__*/React.createElement("span", {
    "aria-hidden": "true",
    style: {
      width: 20,
      height: 20,
      flex: '0 0 auto',
      borderRadius: 4,
      boxSizing: 'border-box',
      display: 'inline-flex',
      alignItems: 'center',
      justifyContent: 'center',
      border: checked ? '2px solid var(--ov-primary)' : '2px solid var(--ov-outline)',
      background: checked ? 'var(--ov-primary)' : 'transparent',
      transition: 'background 120ms ease, border-color 120ms ease'
    }
  }, checked ? /*#__PURE__*/React.createElement("span", {
    className: "material-symbols-outlined",
    style: {
      fontSize: 16,
      color: 'var(--ov-on-primary)'
    }
  }, "check") : null);
  return /*#__PURE__*/React.createElement("label", _extends({
    role: "checkbox",
    "aria-checked": checked,
    onClick: toggle,
    style: {
      display: 'inline-flex',
      alignItems: 'center',
      gap: 12,
      cursor: disabled ? 'not-allowed' : 'pointer',
      opacity: disabled ? 0.38 : 1,
      ...style
    }
  }, rest), box, label ? /*#__PURE__*/React.createElement("span", {
    style: {
      font: 'var(--ov-weight-regular) var(--ov-body-lg-size)/var(--ov-body-lg-line) var(--ov-font-sans)',
      color: 'var(--ov-on-surface)'
    }
  }, label) : null);
}
Object.assign(__ds_scope, { Checkbox });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/forms/Checkbox.jsx", error: String((e && e.message) || e) }); }

// components/forms/RadioGroup.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/**
 * RadioGroup — Material 3 radio list. Each option is a circle (outline ring
 * when unselected, primary ring + filled dot when selected) with a label.
 * Controlled via value/onChange over an options array.
 */
function RadioGroup({
  options = [],
  value,
  onChange,
  disabled = false,
  style,
  ...rest
}) {
  return /*#__PURE__*/React.createElement("div", _extends({
    role: "radiogroup",
    style: {
      display: 'flex',
      flexDirection: 'column',
      gap: 4,
      ...style
    }
  }, rest), options.map(opt => {
    const val = typeof opt === 'string' ? opt : opt.value;
    const label = typeof opt === 'string' ? opt : opt.label;
    const selected = val === value;
    return /*#__PURE__*/React.createElement("label", {
      key: val,
      role: "radio",
      "aria-checked": selected,
      onClick: () => {
        if (!disabled && onChange) onChange(val);
      },
      style: {
        display: 'flex',
        alignItems: 'center',
        gap: 12,
        padding: '8px 0',
        cursor: disabled ? 'not-allowed' : 'pointer',
        opacity: disabled ? 0.38 : 1
      }
    }, /*#__PURE__*/React.createElement("span", {
      "aria-hidden": "true",
      style: {
        width: 20,
        height: 20,
        flex: '0 0 auto',
        borderRadius: 999,
        boxSizing: 'border-box',
        display: 'inline-flex',
        alignItems: 'center',
        justifyContent: 'center',
        border: `2px solid ${selected ? 'var(--ov-primary)' : 'var(--ov-outline)'}`,
        transition: 'border-color 120ms ease'
      }
    }, selected ? /*#__PURE__*/React.createElement("span", {
      style: {
        width: 10,
        height: 10,
        borderRadius: 999,
        background: 'var(--ov-primary)'
      }
    }) : null), /*#__PURE__*/React.createElement("span", {
      style: {
        font: 'var(--ov-weight-regular) var(--ov-body-lg-size)/var(--ov-body-lg-line) var(--ov-font-sans)',
        color: 'var(--ov-on-surface)'
      }
    }, label));
  }));
}
Object.assign(__ds_scope, { RadioGroup });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/forms/RadioGroup.jsx", error: String((e && e.message) || e) }); }

// components/forms/Select.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/**
 * Select — Material 3 outlined dropdown. A styled native <select> with a
 * trailing chevron. Used for units, language, theme mode, favorite activity.
 */
function Select({
  value,
  onChange,
  options = [],
  label,
  disabled = false,
  style,
  ...rest
}) {
  return /*#__PURE__*/React.createElement("label", {
    style: {
      display: 'block',
      ...style
    }
  }, label ? /*#__PURE__*/React.createElement("span", {
    style: {
      display: 'block',
      marginBottom: 6,
      font: 'var(--ov-weight-medium) var(--ov-label-md-size)/var(--ov-label-md-line) var(--ov-font-sans)',
      color: 'var(--ov-on-surface-variant)'
    }
  }, label) : null, /*#__PURE__*/React.createElement("div", {
    style: {
      position: 'relative',
      opacity: disabled ? 0.5 : 1
    }
  }, /*#__PURE__*/React.createElement("select", _extends({
    value: value,
    disabled: disabled,
    onChange: e => onChange && onChange(e.target.value),
    style: {
      width: '100%',
      height: 52,
      appearance: 'none',
      WebkitAppearance: 'none',
      border: '1px solid var(--ov-outline-variant)',
      borderRadius: 'var(--ov-radius-sm)',
      padding: '0 44px 0 14px',
      background: 'transparent',
      font: 'var(--ov-weight-regular) var(--ov-body-lg-size)/1 var(--ov-font-sans)',
      color: 'var(--ov-on-surface)',
      cursor: disabled ? 'not-allowed' : 'pointer'
    }
  }, rest), options.map(opt => {
    const val = typeof opt === 'string' ? opt : opt.value;
    const lbl = typeof opt === 'string' ? opt : opt.label;
    return /*#__PURE__*/React.createElement("option", {
      key: val,
      value: val
    }, lbl);
  })), /*#__PURE__*/React.createElement("span", {
    className: "material-symbols-outlined",
    "aria-hidden": "true",
    style: {
      position: 'absolute',
      right: 14,
      top: '50%',
      transform: 'translateY(-50%)',
      fontSize: 24,
      color: 'var(--ov-on-surface-variant)',
      pointerEvents: 'none'
    }
  }, "expand_more")));
}
Object.assign(__ds_scope, { Select });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/forms/Select.jsx", error: String((e && e.message) || e) }); }

// components/forms/Slider.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/**
 * Slider — Material 3 continuous slider (accent filled track + thumb). Used for
 * calibration/goal inputs (hydration goal, sleep range, caffeine amount).
 * Controlled via value/onChange; renders the current value if valueLabel given.
 */
function Slider({
  value = 0,
  min = 0,
  max = 100,
  step = 1,
  onChange,
  disabled = false,
  accentColor = 'var(--ov-primary)',
  valueLabel,
  style,
  ...rest
}) {
  const frac = Math.max(0, Math.min(1, (value - min) / (max - min || 1)));
  return /*#__PURE__*/React.createElement("div", _extends({
    style: {
      ...style
    }
  }, rest), valueLabel != null ? /*#__PURE__*/React.createElement("div", {
    style: {
      textAlign: 'right',
      marginBottom: 6,
      font: 'var(--ov-weight-semibold) var(--ov-title-md-size)/1 var(--ov-font-sans)',
      color: 'var(--ov-on-surface)'
    }
  }, valueLabel) : null, /*#__PURE__*/React.createElement("div", {
    style: {
      position: 'relative',
      height: 20,
      display: 'flex',
      alignItems: 'center'
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      position: 'absolute',
      left: 0,
      right: 0,
      height: 4,
      borderRadius: 999,
      background: 'var(--ov-surface-container-highest)'
    }
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      position: 'absolute',
      left: 0,
      width: `${frac * 100}%`,
      height: 4,
      borderRadius: 999,
      background: accentColor
    }
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      position: 'absolute',
      left: `calc(${frac * 100}% - 9px)`,
      width: 18,
      height: 18,
      borderRadius: 999,
      background: accentColor,
      boxShadow: '0 1px 3px rgba(0,0,0,.25)'
    }
  }), /*#__PURE__*/React.createElement("input", {
    type: "range",
    value: value,
    min: min,
    max: max,
    step: step,
    disabled: disabled,
    onChange: e => onChange && onChange(Number(e.target.value)),
    style: {
      position: 'absolute',
      left: 0,
      right: 0,
      width: '100%',
      margin: 0,
      height: 20,
      opacity: 0,
      cursor: disabled ? 'not-allowed' : 'pointer'
    }
  })));
}
Object.assign(__ds_scope, { Slider });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/forms/Slider.jsx", error: String((e && e.message) || e) }); }

// components/forms/Switch.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/**
 * Switch — Material 3 toggle. Off: outline track + outline thumb. On: primary
 * track + on-primary thumb that grows and slides right. Used for settings
 * toggles (reminders, keep-screen-on, high-contrast mode…).
 */
function Switch({
  checked = false,
  onChange,
  disabled = false,
  label,
  style,
  ...rest
}) {
  const toggle = () => {
    if (!disabled && onChange) onChange(!checked);
  };
  const control = /*#__PURE__*/React.createElement("button", _extends({
    type: "button",
    role: "switch",
    "aria-checked": checked,
    "aria-label": label,
    onClick: toggle,
    disabled: disabled,
    style: {
      width: 52,
      height: 32,
      borderRadius: 999,
      flex: '0 0 auto',
      position: 'relative',
      border: checked ? '2px solid transparent' : '2px solid var(--ov-outline)',
      background: checked ? 'var(--ov-primary)' : 'var(--ov-surface-container-highest)',
      cursor: disabled ? 'not-allowed' : 'pointer',
      opacity: disabled ? 0.38 : 1,
      transition: 'background 140ms ease',
      boxSizing: 'border-box',
      padding: 0
    }
  }, rest), /*#__PURE__*/React.createElement("span", {
    style: {
      position: 'absolute',
      top: '50%',
      left: checked ? 26 : 6,
      transform: 'translate(-0%, -50%)',
      width: checked ? 22 : 14,
      height: checked ? 22 : 14,
      borderRadius: 999,
      background: checked ? 'var(--ov-on-primary)' : 'var(--ov-outline)',
      transition: 'left 140ms ease, width 140ms ease, height 140ms ease'
    }
  }));
  if (!label) return control;
  return /*#__PURE__*/React.createElement("label", {
    style: {
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'space-between',
      gap: 16,
      cursor: disabled ? 'not-allowed' : 'pointer',
      ...style
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      font: 'var(--ov-weight-regular) var(--ov-body-lg-size)/var(--ov-body-lg-line) var(--ov-font-sans)',
      color: 'var(--ov-on-surface)'
    }
  }, label), control);
}
Object.assign(__ds_scope, { Switch });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/forms/Switch.jsx", error: String((e && e.message) || e) }); }

// components/forms/TextField.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/**
 * TextField — Material 3 outlined text field. Floating-ish label above a
 * rounded outlined input. Used for search ("Search drinks"), custom amounts,
 * and manual-entry values. Controlled via value/onChange.
 */
function TextField({
  value = '',
  onChange,
  label,
  placeholder,
  type = 'text',
  disabled = false,
  leadingIcon,
  suffix,
  style,
  ...rest
}) {
  return /*#__PURE__*/React.createElement("label", {
    style: {
      display: 'block',
      ...style
    }
  }, label ? /*#__PURE__*/React.createElement("span", {
    style: {
      display: 'block',
      marginBottom: 6,
      font: 'var(--ov-weight-medium) var(--ov-label-md-size)/var(--ov-label-md-line) var(--ov-font-sans)',
      color: 'var(--ov-on-surface-variant)'
    }
  }, label) : null, /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      alignItems: 'center',
      gap: 10,
      border: '1px solid var(--ov-outline-variant)',
      borderRadius: 'var(--ov-radius-sm)',
      padding: '0 14px',
      height: 52,
      background: 'transparent',
      opacity: disabled ? 0.5 : 1
    }
  }, leadingIcon ? /*#__PURE__*/React.createElement("span", {
    className: "material-symbols-outlined",
    style: {
      fontSize: 20,
      color: 'var(--ov-on-surface-variant)'
    }
  }, leadingIcon) : null, /*#__PURE__*/React.createElement("input", _extends({
    type: type,
    value: value,
    placeholder: placeholder,
    disabled: disabled,
    onChange: e => onChange && onChange(e.target.value),
    style: {
      flex: 1,
      minWidth: 0,
      border: 'none',
      outline: 'none',
      background: 'transparent',
      font: 'var(--ov-weight-regular) var(--ov-body-lg-size)/1.4 var(--ov-font-sans)',
      color: 'var(--ov-on-surface)'
    }
  }, rest)), suffix ? /*#__PURE__*/React.createElement("span", {
    style: {
      font: 'var(--ov-weight-medium) var(--ov-body-md-size)/1 var(--ov-font-sans)',
      color: 'var(--ov-on-surface-variant)'
    }
  }, suffix) : null));
}
Object.assign(__ds_scope, { TextField });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/forms/TextField.jsx", error: String((e && e.message) || e) }); }

// components/insights/AchievementBadge.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/**
 * AchievementBadge — a Fitbit-inspired achievement card (features/achievements/
 * AchievementsContent.kt · AchievementBadgeCard). A 48px accent icon badge
 * (accent at 20% when unlocked, 10% when locked), name + lock/check glyph,
 * requirement text, an 8px progress bar, and a current/target + status row.
 * Unlocked cards tint their whole surface with the accent at 12%.
 */
function AchievementBadge({
  icon = 'directions_walk',
  name,
  requirement,
  current,
  target,
  progress = 0,
  unlocked = false,
  status,
  // e.g. "Achieved 2 Jul" / "Locked"
  accentColor = 'var(--ov-metric-steps)',
  style,
  ...rest
}) {
  const frac = Math.max(0, Math.min(1, progress));
  return /*#__PURE__*/React.createElement("div", _extends({
    style: {
      background: unlocked ? `color-mix(in srgb, ${accentColor} 12%, var(--ov-surface-container))` : 'var(--ov-surface-container)',
      borderRadius: 'var(--ov-radius-md)',
      padding: 16,
      ...style
    }
  }, rest), /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      alignItems: 'center',
      gap: 12
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      width: 48,
      height: 48,
      flex: '0 0 auto',
      borderRadius: 'var(--ov-radius-full)',
      background: `color-mix(in srgb, ${accentColor} ${unlocked ? 20 : 10}%, transparent)`,
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center'
    }
  }, /*#__PURE__*/React.createElement("span", {
    className: "material-symbols-outlined",
    style: {
      fontSize: 26,
      color: accentColor
    },
    "aria-hidden": "true"
  }, icon)), /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1,
      minWidth: 0
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      alignItems: 'center',
      gap: 8
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      flex: 1,
      font: 'var(--ov-weight-semibold) var(--ov-title-md-size)/var(--ov-title-md-line) var(--ov-font-sans)',
      color: 'var(--ov-on-surface)'
    }
  }, name), /*#__PURE__*/React.createElement("span", {
    className: "material-symbols-outlined",
    style: {
      fontSize: 20,
      color: unlocked ? accentColor : 'var(--ov-on-surface-variant)'
    },
    "aria-hidden": "true"
  }, unlocked ? 'check_circle' : 'lock')), requirement ? /*#__PURE__*/React.createElement("div", {
    style: {
      font: 'var(--ov-weight-regular) var(--ov-body-md-size)/var(--ov-body-md-line) var(--ov-font-sans)',
      color: 'var(--ov-on-surface-variant)'
    }
  }, requirement) : null)), /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 12,
      height: 8,
      borderRadius: 999,
      background: 'var(--ov-surface-container-highest)',
      overflow: 'hidden'
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      height: '100%',
      width: `${frac * 100}%`,
      background: accentColor
    }
  })), /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 8,
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'space-between',
      gap: 8
    }
  }, current != null && target != null ? /*#__PURE__*/React.createElement("span", {
    style: {
      font: 'var(--ov-weight-regular) var(--ov-body-sm-size)/var(--ov-body-sm-line) var(--ov-font-sans)',
      color: 'var(--ov-on-surface-variant)'
    }
  }, current, " / ", target) : /*#__PURE__*/React.createElement("span", null), status ? /*#__PURE__*/React.createElement("span", {
    style: {
      font: 'var(--ov-weight-medium) var(--ov-label-md-size)/var(--ov-label-md-line) var(--ov-font-sans)',
      color: unlocked ? accentColor : 'var(--ov-on-surface-variant)'
    }
  }, status) : null));
}
Object.assign(__ds_scope, { AchievementBadge });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/insights/AchievementBadge.jsx", error: String((e && e.message) || e) }); }

// components/insights/CrossMetricInsightCard.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/**
 * CrossMetricInsightCard — the correlation insight card (ui/components/
 * CrossMetricInsightCard.kt). A 1px outline-variant bordered card: a trend
 * glyph (up / down / flat) in the accent, a title + relationship line, a bold
 * signed correlation % on the right, then a message and paired-days note.
 */
function CrossMetricInsightCard({
  title,
  direction = 'flat',
  // 'positive' | 'negative' | 'flat'
  correlation = 0,
  // percent, e.g. 62 or -41
  relationship,
  // e.g. "Positive link"
  message,
  pairedDays,
  accentColor = 'var(--ov-primary)',
  style,
  ...rest
}) {
  const icon = direction === 'positive' ? 'trending_up' : direction === 'negative' ? 'trending_down' : 'trending_flat';
  const rel = relationship || (direction === 'positive' ? 'Positive link' : direction === 'negative' ? 'Negative link' : 'Weak link');
  const pct = Math.round(correlation);
  const signed = pct > 0 ? `+${pct}%` : pct < 0 ? `${pct}%` : '0%';
  return /*#__PURE__*/React.createElement("div", _extends({
    style: {
      background: 'var(--ov-surface-container)',
      border: '1px solid color-mix(in srgb, var(--ov-outline-variant) 70%, transparent)',
      borderRadius: 'var(--ov-radius-md)',
      padding: 16,
      ...style
    }
  }, rest), /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      alignItems: 'flex-start',
      justifyContent: 'space-between',
      gap: 12
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      alignItems: 'center',
      gap: 12,
      minWidth: 0,
      flex: 1
    }
  }, /*#__PURE__*/React.createElement("span", {
    className: "material-symbols-outlined",
    style: {
      fontSize: 24,
      color: accentColor,
      flex: '0 0 auto'
    },
    "aria-hidden": "true"
  }, icon), /*#__PURE__*/React.createElement("div", {
    style: {
      minWidth: 0
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: 'var(--ov-weight-semibold) var(--ov-title-sm-size)/var(--ov-title-sm-line) var(--ov-font-sans)',
      color: 'var(--ov-on-surface)'
    }
  }, title), /*#__PURE__*/React.createElement("div", {
    style: {
      font: 'var(--ov-weight-regular) var(--ov-body-sm-size)/var(--ov-body-sm-line) var(--ov-font-sans)',
      color: 'var(--ov-on-surface-variant)'
    }
  }, rel))), /*#__PURE__*/React.createElement("div", {
    style: {
      font: 'var(--ov-weight-bold) var(--ov-title-md-size)/var(--ov-title-md-line) var(--ov-font-sans)',
      color: 'var(--ov-on-surface)',
      fontFeatureSettings: "'tnum'",
      flex: '0 0 auto'
    }
  }, signed)), message ? /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 12,
      font: 'var(--ov-weight-regular) var(--ov-body-md-size)/var(--ov-body-md-line) var(--ov-font-sans)',
      color: 'var(--ov-on-surface-variant)'
    }
  }, message) : null, pairedDays != null ? /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 8,
      font: 'var(--ov-weight-medium) var(--ov-label-md-size)/var(--ov-label-md-line) var(--ov-font-sans)',
      color: 'var(--ov-on-surface-variant)'
    }
  }, "Based on ", pairedDays, " paired days") : null);
}
Object.assign(__ds_scope, { CrossMetricInsightCard });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/insights/CrossMetricInsightCard.jsx", error: String((e && e.message) || e) }); }

// components/insights/DataConfidenceCard.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/**
 * DataConfidenceCard — the bordered "Data confidence" card (ui/components/
 * DataConfidenceCard.kt). A 1px border tinted by confidence level (high =
 * accent, medium = tertiary/green, low = error), a Verified icon + title +
 * bold level word, then coverage / samples / source / value-kind lines and
 * up to three "- warning" notes.
 */
function DataConfidenceCard({
  level = 'high',
  // 'high' | 'medium' | 'low'
  coverage,
  // e.g. "7 of 7 days tracked (100%)"
  samples,
  // e.g. "14 records"
  source,
  // e.g. "Source: Fitbit"
  valueKind,
  // e.g. "Measured Health Connect records"
  warnings = [],
  accentColor = 'var(--ov-primary)',
  style,
  ...rest
}) {
  const levelColor = level === 'high' ? accentColor : level === 'medium' ? 'var(--ov-tertiary)' : 'var(--ov-error)';
  const levelText = level.charAt(0).toUpperCase() + level.slice(1) + ' confidence';
  const lines = [coverage, samples, source, valueKind].filter(Boolean);
  return /*#__PURE__*/React.createElement("div", _extends({
    style: {
      background: 'var(--ov-surface-container)',
      border: `1px solid color-mix(in srgb, ${levelColor} 25%, transparent)`,
      borderRadius: 'var(--ov-radius-md)',
      padding: 16,
      ...style
    }
  }, rest), /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      alignItems: 'center',
      gap: 12
    }
  }, /*#__PURE__*/React.createElement("span", {
    className: "material-symbols-outlined",
    style: {
      fontSize: 22,
      color: levelColor
    },
    "aria-hidden": "true"
  }, "verified"), /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("div", {
    style: {
      font: 'var(--ov-weight-semibold) var(--ov-title-sm-size)/var(--ov-title-sm-line) var(--ov-font-sans)',
      color: 'var(--ov-on-surface)'
    }
  }, "Data confidence"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: 'var(--ov-weight-bold) var(--ov-label-lg-size)/var(--ov-label-lg-line) var(--ov-font-sans)',
      letterSpacing: 'var(--ov-label-lg-tracking)',
      color: levelColor
    }
  }, levelText))), /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 12,
      display: 'flex',
      flexDirection: 'column',
      gap: 0
    }
  }, lines.map((l, i) => /*#__PURE__*/React.createElement("div", {
    key: i,
    style: {
      font: 'var(--ov-weight-regular) var(--ov-body-md-size)/var(--ov-body-md-line) var(--ov-font-sans)',
      color: 'var(--ov-on-surface-variant)'
    }
  }, l)), warnings.slice(0, 3).map((w, i) => /*#__PURE__*/React.createElement("div", {
    key: i,
    style: {
      marginTop: 6,
      font: 'var(--ov-weight-regular) var(--ov-body-sm-size)/var(--ov-body-sm-line) var(--ov-font-sans)',
      color: 'var(--ov-on-surface-variant)'
    }
  }, "- ", w))));
}
Object.assign(__ds_scope, { DataConfidenceCard });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/insights/DataConfidenceCard.jsx", error: String((e && e.message) || e) }); }

// components/insights/SensorStatusCard.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/**
 * SensorStatusCard — the dashboard sensor/battery status row (features/
 * dashboard/DashboardSensorStatusCard.kt). A tappable card with a 40px
 * battery-tinted icon circle, a "Sensors" label + lowest-battery headline, and
 * an "active · connected" supporting line. Accent follows battery: >40 primary,
 * <=40 tertiary, <=20 error, unknown primary.
 */
function SensorStatusCard({
  batteryPercent,
  // number | null
  activeCount = 0,
  connectedCount = 0,
  onClick,
  style,
  ...rest
}) {
  const accent = batteryPercent == null ? 'var(--ov-primary)' : batteryPercent <= 20 ? 'var(--ov-error)' : batteryPercent <= 40 ? 'var(--ov-tertiary)' : 'var(--ov-primary)';
  const headline = batteryPercent == null ? 'Battery level unavailable' : `Lowest battery ${batteryPercent}%`;
  const supporting = activeCount === 0 ? 'All sensors disabled' : `${activeCount} active · ${connectedCount} connected`;
  const interactive = typeof onClick === 'function';
  return /*#__PURE__*/React.createElement("div", _extends({
    role: interactive ? 'button' : undefined,
    onClick: onClick,
    style: {
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'space-between',
      gap: 8,
      background: 'var(--ov-surface-container)',
      borderRadius: 'var(--ov-radius-md)',
      padding: '12px 14px',
      cursor: interactive ? 'pointer' : 'default',
      boxSizing: 'border-box',
      ...style
    }
  }, rest), /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      alignItems: 'center',
      gap: 12,
      minWidth: 0,
      flex: 1
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      width: 40,
      height: 40,
      flex: '0 0 auto',
      borderRadius: 'var(--ov-radius-full)',
      background: `color-mix(in srgb, ${accent} 14%, transparent)`,
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center'
    }
  }, /*#__PURE__*/React.createElement("span", {
    className: "material-symbols-outlined",
    style: {
      fontSize: 21,
      color: accent
    },
    "aria-hidden": "true"
  }, "battery_charging_full")), /*#__PURE__*/React.createElement("div", {
    style: {
      minWidth: 0
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: 'var(--ov-weight-semibold) var(--ov-label-md-size)/var(--ov-label-md-line) var(--ov-font-sans)',
      color: 'var(--ov-on-surface-variant)'
    }
  }, "Sensors"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: 'var(--ov-weight-semibold) var(--ov-title-md-size)/var(--ov-title-md-line) var(--ov-font-sans)',
      color: 'var(--ov-on-surface)',
      whiteSpace: 'nowrap',
      overflow: 'hidden',
      textOverflow: 'ellipsis'
    }
  }, headline))), /*#__PURE__*/React.createElement("div", {
    style: {
      font: 'var(--ov-weight-semibold) var(--ov-label-md-size)/var(--ov-label-md-line) var(--ov-font-sans)',
      color: 'var(--ov-on-surface-variant)',
      flex: '0 0 auto',
      paddingLeft: 8
    }
  }, supporting));
}
Object.assign(__ds_scope, { SensorStatusCard });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/insights/SensorStatusCard.jsx", error: String((e && e.message) || e) }); }

// components/navigation/BottomNavBar.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/**
 * BottomNavBar — Material 3 navigation bar (OpenVitalsAdaptiveScaffold). Each
 * item is an icon over a label; the selected item gets a secondary-container
 * pill behind its icon and on-secondary-container tint. Controlled via
 * value/onChange over an items array.
 */
function BottomNavBar({
  items = [],
  value,
  onChange,
  style,
  ...rest
}) {
  return /*#__PURE__*/React.createElement("div", _extends({
    style: {
      display: 'flex',
      alignItems: 'stretch',
      height: 'var(--ov-nav-bar-height)',
      background: 'var(--ov-surface-container)',
      ...style
    }
  }, rest), items.map(it => {
    const selected = it.value === value;
    return /*#__PURE__*/React.createElement("button", {
      key: it.value,
      type: "button",
      onClick: () => onChange && onChange(it.value),
      "aria-label": it.label,
      "aria-current": selected,
      style: {
        flex: 1,
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        gap: 4,
        border: 'none',
        background: 'transparent',
        cursor: 'pointer',
        paddingTop: 12
      }
    }, /*#__PURE__*/React.createElement("span", {
      style: {
        display: 'inline-flex',
        alignItems: 'center',
        justifyContent: 'center',
        width: 64,
        height: 32,
        borderRadius: 999,
        background: selected ? 'var(--ov-secondary-container)' : 'transparent',
        transition: 'background 120ms ease'
      }
    }, /*#__PURE__*/React.createElement("span", {
      className: "material-symbols-outlined",
      style: {
        fontSize: 24,
        color: selected ? 'var(--ov-on-secondary-container)' : 'var(--ov-on-surface-variant)',
        fontVariationSettings: `'FILL' ${selected ? 1 : 0}, 'wght' 500, 'opsz' 24`
      },
      "aria-hidden": "true"
    }, it.icon)), /*#__PURE__*/React.createElement("span", {
      style: {
        font: `${selected ? 'var(--ov-weight-semibold)' : 'var(--ov-weight-medium)'} var(--ov-label-md-size)/var(--ov-label-md-line) var(--ov-font-sans)`,
        color: selected ? 'var(--ov-on-surface)' : 'var(--ov-on-surface-variant)'
      }
    }, it.label));
  }));
}
Object.assign(__ds_scope, { BottomNavBar });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/navigation/BottomNavBar.jsx", error: String((e && e.message) || e) }); }

// components/navigation/DateNavigator.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/**
 * DateNavigator — the detail-screen date header (DayNavigator in source):
 * a big day title + subtitle on the left, three circular surface buttons on
 * the right (previous / next / calendar). Next is disabled when canGoForward
 * is false. Matches the header on Dashboard / Daily Readiness / Body Energy.
 */
function DateNavigator({
  title = 'Today',
  subtitle,
  canGoForward = false,
  onPrevious,
  onNext,
  onOpenCalendar,
  style,
  ...rest
}) {
  const CircleBtn = ({
    icon,
    label,
    onClick,
    disabled
  }) => /*#__PURE__*/React.createElement("button", {
    type: "button",
    "aria-label": label,
    onClick: disabled ? undefined : onClick,
    disabled: disabled,
    style: {
      width: 52,
      height: 52,
      borderRadius: 'var(--ov-radius-full)',
      border: 'none',
      background: 'var(--ov-surface-container)',
      color: disabled ? 'color-mix(in srgb, var(--ov-on-surface) 38%, transparent)' : 'var(--ov-on-surface)',
      opacity: disabled ? 0.55 : 1,
      cursor: disabled ? 'not-allowed' : 'pointer',
      display: 'inline-flex',
      alignItems: 'center',
      justifyContent: 'center'
    }
  }, /*#__PURE__*/React.createElement("span", {
    className: "material-symbols-outlined",
    style: {
      fontSize: 24
    },
    "aria-hidden": "true"
  }, icon));
  return /*#__PURE__*/React.createElement("div", _extends({
    style: {
      display: 'flex',
      alignItems: 'center',
      gap: 12,
      ...style
    }
  }, rest), /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1,
      minWidth: 0,
      cursor: onOpenCalendar ? 'pointer' : 'default'
    },
    onClick: onOpenCalendar
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: 'var(--ov-weight-semibold) var(--ov-title-lg-size)/var(--ov-title-lg-line) var(--ov-font-sans)',
      color: 'var(--ov-on-surface)'
    }
  }, title), subtitle ? /*#__PURE__*/React.createElement("div", {
    style: {
      font: 'var(--ov-weight-regular) var(--ov-body-sm-size)/var(--ov-body-sm-line) var(--ov-font-sans)',
      color: 'var(--ov-on-surface-variant)'
    }
  }, subtitle) : null), /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      alignItems: 'center',
      gap: 8
    }
  }, /*#__PURE__*/React.createElement(CircleBtn, {
    icon: "chevron_left",
    label: "Previous",
    onClick: onPrevious
  }), /*#__PURE__*/React.createElement(CircleBtn, {
    icon: "chevron_right",
    label: "Next",
    onClick: onNext,
    disabled: !canGoForward
  }), /*#__PURE__*/React.createElement(CircleBtn, {
    icon: "calendar_month",
    label: "Open calendar",
    onClick: onOpenCalendar
  })));
}
Object.assign(__ds_scope, { DateNavigator });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/navigation/DateNavigator.jsx", error: String((e && e.message) || e) }); }

// components/navigation/SectionHeader.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/**
 * SectionHeader — a lightweight list/section label (titleSmall,
 * on-surface-variant) as in ui/components/SectionHeader.kt. Optional trailing
 * affordance (e.g. a chevron "see all" link).
 */
function SectionHeader({
  text,
  trailing,
  onTrailingClick,
  style,
  ...rest
}) {
  return /*#__PURE__*/React.createElement("div", _extends({
    style: {
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'space-between',
      padding: '8px 0',
      ...style
    }
  }, rest), /*#__PURE__*/React.createElement("span", {
    style: {
      font: 'var(--ov-weight-medium) var(--ov-title-sm-size)/var(--ov-title-sm-line) var(--ov-font-sans)',
      color: 'var(--ov-on-surface-variant)'
    }
  }, text), trailing ? /*#__PURE__*/React.createElement("button", {
    type: "button",
    onClick: onTrailingClick,
    "aria-label": typeof trailing === 'string' ? trailing : 'More',
    style: {
      border: 'none',
      background: 'transparent',
      cursor: 'pointer',
      color: 'var(--ov-on-surface-variant)',
      display: 'inline-flex',
      alignItems: 'center'
    }
  }, typeof trailing === 'string' ? /*#__PURE__*/React.createElement("span", {
    className: "material-symbols-outlined",
    style: {
      fontSize: 22
    },
    "aria-hidden": "true"
  }, trailing) : trailing) : null);
}
Object.assign(__ds_scope, { SectionHeader });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/navigation/SectionHeader.jsx", error: String((e && e.message) || e) }); }

// components/navigation/TimeRangeSelector.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/**
 * TimeRangeSelector — the Day / Week / Month / Year segmented control from
 * ui/components/TimeRangeSelector.kt. Each segment is a large-radius (24px)
 * pill; the selected one uses primaryContainer, the rest surfaceContainer.
 */
function TimeRangeSelector({
  options = ['Day', 'Week', 'Month', 'Year'],
  value,
  onChange,
  style,
  ...rest
}) {
  const selected = value ?? options[0];
  return /*#__PURE__*/React.createElement("div", _extends({
    style: {
      display: 'flex',
      gap: 4,
      ...style
    }
  }, rest), options.map(opt => {
    const isSel = opt === selected;
    return /*#__PURE__*/React.createElement("button", {
      key: opt,
      type: "button",
      onClick: () => onChange && onChange(opt),
      style: {
        flex: 1,
        padding: '12px 8px',
        borderRadius: 'var(--ov-radius-lg)',
        border: 'none',
        cursor: 'pointer',
        background: isSel ? 'var(--ov-primary-container)' : 'var(--ov-surface-container)',
        color: isSel ? 'var(--ov-on-primary-container)' : 'var(--ov-on-surface-variant)',
        font: `${isSel ? 'var(--ov-weight-semibold)' : 'var(--ov-weight-medium)'} var(--ov-label-lg-size)/var(--ov-label-lg-line) var(--ov-font-sans)`,
        letterSpacing: 'var(--ov-label-lg-tracking)',
        transition: 'background 120ms ease'
      }
    }, opt);
  }));
}
Object.assign(__ds_scope, { TimeRangeSelector });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/navigation/TimeRangeSelector.jsx", error: String((e && e.message) || e) }); }

// components/navigation/TopBar.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/**
 * TopBar — the app's Material 3 top app bar. Two forms:
 *  - Home (large title flush-left, e.g. "OpenVitals") with trailing action icons.
 *  - Detail (back chevron + title, e.g. "Daily Readiness") with optional actions.
 * Transparent over the app background; no divider.
 */
function TopBar({
  title,
  onBack,
  actions = [],
  large = false,
  style,
  ...rest
}) {
  return /*#__PURE__*/React.createElement("div", _extends({
    style: {
      display: 'flex',
      alignItems: 'center',
      gap: 8,
      minHeight: 'var(--ov-top-bar-height)',
      padding: '8px 8px 8px 16px',
      background: 'transparent',
      ...style
    }
  }, rest), onBack ? /*#__PURE__*/React.createElement("button", {
    type: "button",
    "aria-label": "Back",
    onClick: onBack,
    style: {
      width: 44,
      height: 44,
      borderRadius: 'var(--ov-radius-full)',
      border: 'none',
      background: 'transparent',
      color: 'var(--ov-on-surface)',
      cursor: 'pointer',
      display: 'inline-flex',
      alignItems: 'center',
      justifyContent: 'center',
      marginRight: 4
    }
  }, /*#__PURE__*/React.createElement("span", {
    className: "material-symbols-outlined",
    style: {
      fontSize: 24
    },
    "aria-hidden": "true"
  }, "arrow_back")) : null, /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1,
      minWidth: 0,
      font: large ? 'var(--ov-weight-bold) var(--ov-headline-lg-size)/var(--ov-headline-lg-line) var(--ov-font-sans)' : 'var(--ov-weight-semibold) var(--ov-title-lg-size)/var(--ov-title-lg-line) var(--ov-font-sans)',
      color: 'var(--ov-on-surface)',
      whiteSpace: 'nowrap',
      overflow: 'hidden',
      textOverflow: 'ellipsis'
    }
  }, title), /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      alignItems: 'center',
      gap: 2
    }
  }, actions.map((a, i) => /*#__PURE__*/React.createElement("button", {
    key: i,
    type: "button",
    "aria-label": a.label,
    title: a.label,
    onClick: a.onClick,
    style: {
      width: 44,
      height: 44,
      borderRadius: 'var(--ov-radius-full)',
      border: 'none',
      background: 'transparent',
      color: 'var(--ov-on-surface)',
      cursor: 'pointer',
      display: 'inline-flex',
      alignItems: 'center',
      justifyContent: 'center'
    }
  }, /*#__PURE__*/React.createElement("span", {
    className: "material-symbols-outlined",
    style: {
      fontSize: 24,
      fontVariationSettings: "'FILL' 0, 'wght' 500, 'opsz' 24"
    },
    "aria-hidden": "true"
  }, a.icon)))));
}
Object.assign(__ds_scope, { TopBar });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/navigation/TopBar.jsx", error: String((e && e.message) || e) }); }

// ui_kits/openvitals-app/AchievementsScreen.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
// AchievementsScreen — badge progress list.
const {
  TopBar,
  Card,
  AchievementBadge
} = window.OpenVitalsDesignSystem_626946;
function AchievementsScreen({
  nav
}) {
  const [filter, setFilter] = React.useState('All');
  const filters = ['All', 'Steps', 'Distance', 'Floors'];
  const badges = [{
    icon: 'directions_walk',
    name: 'High Tops',
    requirement: 'Walk 20,000 steps in a day',
    current: '19,576',
    target: '20,000',
    progress: 0.98,
    unlocked: false,
    status: 'Almost there',
    accentColor: 'var(--ov-metric-steps)',
    cat: 'Steps'
  }, {
    icon: 'directions_walk',
    name: 'Sneakers',
    requirement: 'Walk 10,000 steps in a day',
    current: '19,576',
    target: '10,000',
    progress: 1,
    unlocked: true,
    status: 'Achieved 28 Jun',
    accentColor: 'var(--ov-metric-steps)',
    cat: 'Steps'
  }, {
    icon: 'straighten',
    name: 'Marathon',
    requirement: 'Walk 26 mi in total',
    current: '41.2 mi',
    target: '26 mi',
    progress: 1,
    unlocked: true,
    status: 'Achieved 2 Jul',
    accentColor: 'var(--ov-metric-distance)',
    cat: 'Distance'
  }, {
    icon: 'straighten',
    name: 'London Underground',
    requirement: 'Walk 250 mi in total',
    current: '41.2 mi',
    target: '250 mi',
    progress: 0.16,
    unlocked: false,
    status: 'Locked',
    accentColor: 'var(--ov-metric-distance)',
    cat: 'Distance'
  }, {
    icon: 'stairs',
    name: 'Skyscraper',
    requirement: 'Climb 100 floors in a day',
    current: '62',
    target: '100',
    progress: 0.62,
    unlocked: false,
    status: 'Locked',
    accentColor: 'var(--ov-metric-floors)',
    cat: 'Floors'
  }];
  const shown = badges.filter(b => filter === 'All' || b.cat === filter);
  const unlocked = badges.filter(b => b.unlocked).length;
  return /*#__PURE__*/React.createElement("div", {
    style: {
      paddingBottom: 32
    }
  }, /*#__PURE__*/React.createElement(TopBar, {
    title: "Achievements",
    onBack: () => nav('dashboard')
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      padding: '4px 16px 0'
    }
  }, /*#__PURE__*/React.createElement(Card, {
    padding: 16
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      alignItems: 'center',
      gap: 12
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      width: 48,
      height: 48,
      borderRadius: 999,
      flex: '0 0 auto',
      background: 'color-mix(in srgb, var(--ov-primary) 20%, transparent)',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center'
    }
  }, /*#__PURE__*/React.createElement("span", {
    className: "material-symbols-outlined",
    style: {
      fontSize: 26,
      color: 'var(--ov-primary)'
    }
  }, "workspace_premium")), /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: 'var(--ov-weight-semibold) var(--ov-title-lg-size)/1.2 var(--ov-font-sans)',
      color: 'var(--ov-on-surface)'
    }
  }, "Achievements"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: 'var(--ov-weight-regular) var(--ov-body-md-size)/1.3 var(--ov-font-sans)',
      color: 'var(--ov-on-surface-variant)'
    }
  }, unlocked, " of ", badges.length, " unlocked"))), /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 12,
      height: 8,
      borderRadius: 999,
      background: 'var(--ov-surface-container-highest)',
      overflow: 'hidden'
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      height: '100%',
      width: `${unlocked / badges.length * 100}%`,
      background: 'var(--ov-primary)'
    }
  }))), /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      gap: 8,
      margin: '14px 0 6px',
      flexWrap: 'wrap'
    }
  }, filters.map(f => {
    const sel = f === filter;
    return /*#__PURE__*/React.createElement("button", {
      key: f,
      onClick: () => setFilter(f),
      style: {
        padding: '7px 14px',
        borderRadius: 999,
        cursor: 'pointer',
        border: `1px solid ${sel ? 'transparent' : 'var(--ov-outline-variant)'}`,
        background: sel ? 'var(--ov-secondary-container)' : 'transparent',
        color: sel ? 'var(--ov-on-secondary-container)' : 'var(--ov-on-surface-variant)',
        font: 'var(--ov-weight-medium) var(--ov-label-lg-size)/1 var(--ov-font-sans)'
      }
    }, f);
  })), /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      flexDirection: 'column',
      gap: 12,
      marginTop: 8
    }
  }, shown.map((b, i) => /*#__PURE__*/React.createElement(AchievementBadge, _extends({
    key: i
  }, b))))));
}
Object.assign(window, {
  AchievementsScreen
});
})(); } catch (e) { __ds_ns.__errors.push({ path: "ui_kits/openvitals-app/AchievementsScreen.jsx", error: String((e && e.message) || e) }); }

// ui_kits/openvitals-app/ActivityDetailScreen.jsx
try { (() => {
// ActivityDetailScreen — a recorded walk. Composes TopBar, Card, DetailRow, AccentIconChip.
const {
  TopBar,
  Card,
  DetailRow,
  AccentIconChip
} = window.OpenVitalsDesignSystem_626946;
function ActivityDetailScreen({
  nav
}) {
  const metrics = [['Duration', '2h 46m'], ['Steps', '14,024'], ['Distance', '10.0 mi'], ['Average pace', '16:43 min/mi'], ['Average speed', '3.6 mph'], ['Recorded speed', '4.5 mph'], ['Average heart rate', '107 bpm'], ['Average power', 'Not available'], ['Step cadence', '79.3 rpm'], ['Total calories burned', '833 kcal'], ['Active calories', '562 kcal'], ['Floors climbed', '0'], ['Elevation gained', '276 ft']];
  return /*#__PURE__*/React.createElement("div", {
    style: {
      paddingBottom: 32
    }
  }, /*#__PURE__*/React.createElement(TopBar, {
    title: "Activity detail",
    onBack: () => nav('dashboard')
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      padding: '4px 16px 0'
    }
  }, /*#__PURE__*/React.createElement(Card, {
    padding: 16
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      alignItems: 'flex-start',
      justifyContent: 'space-between'
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      alignItems: 'center',
      gap: 12
    }
  }, /*#__PURE__*/React.createElement(AccentIconChip, {
    icon: "directions_walk",
    color: "var(--ov-metric-workout)",
    size: 40
  }), /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("div", {
    style: {
      font: 'var(--ov-weight-bold) var(--ov-headline-sm-size)/1.1 var(--ov-font-sans)',
      color: 'var(--ov-on-surface)'
    }
  }, "Walk"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: 'var(--ov-weight-regular) var(--ov-body-md-size)/1.3 var(--ov-font-sans)',
      color: 'var(--ov-on-surface-variant)'
    }
  }, "Walking"))), /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'inline-flex',
      alignItems: 'center',
      gap: 8,
      padding: '6px 12px',
      borderRadius: 999,
      border: '1px solid var(--ov-outline-variant)'
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      width: 14,
      height: 14,
      borderRadius: '50%',
      background: 'var(--ov-outline)'
    }
  }), /*#__PURE__*/React.createElement("span", {
    style: {
      font: 'var(--ov-weight-medium) var(--ov-label-md-size)/1 var(--ov-font-sans)',
      color: 'var(--ov-on-surface-variant)'
    }
  }, "Gadgetbridge"))), /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 14,
      font: 'var(--ov-weight-bold) var(--ov-headline-lg-size)/1 var(--ov-font-sans)',
      color: 'var(--ov-metric-workout)',
      fontFeatureSettings: "'tnum'"
    }
  }, "2h 46m"), /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 6,
      font: 'var(--ov-weight-regular) var(--ov-body-md-size)/1.4 var(--ov-font-sans)',
      color: 'var(--ov-on-surface-variant)'
    }
  }, "Jun 28, 2026, 10:33 AM \u2013 1:20 PM")), /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 12
    }
  }, /*#__PURE__*/React.createElement(Card, {
    padding: 16
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: 'var(--ov-weight-bold) var(--ov-title-md-size)/1.3 var(--ov-font-sans)',
      color: 'var(--ov-on-surface)',
      marginBottom: 8
    }
  }, "Metrics"), /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      flexDirection: 'column',
      gap: 8
    }
  }, metrics.map(([l, v]) => /*#__PURE__*/React.createElement(DetailRow, {
    key: l,
    label: l,
    value: v
  }))))), /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 12
    }
  }, /*#__PURE__*/React.createElement(Card, {
    variant: "accent",
    accentColor: "var(--ov-metric-heart)",
    padding: 16
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: 'var(--ov-weight-bold) var(--ov-title-md-size)/1.3 var(--ov-font-sans)',
      color: 'var(--ov-on-surface)',
      marginBottom: 12
    }
  }, "Heart rate"), /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      gap: 12
    }
  }, [['108 bpm', 'Avg'], ['68–140 bpm', 'Range'], ['1,566', 'Samples']].map(([v, l]) => /*#__PURE__*/React.createElement("div", {
    key: l,
    style: {
      flex: 1
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: 'var(--ov-weight-bold) var(--ov-title-lg-size)/1.1 var(--ov-font-sans)',
      color: 'var(--ov-metric-heart)'
    }
  }, v), /*#__PURE__*/React.createElement("div", {
    style: {
      font: 'var(--ov-weight-medium) var(--ov-label-md-size)/1.3 var(--ov-font-sans)',
      color: 'var(--ov-on-surface-variant)'
    }
  }, l))))))));
}
Object.assign(window, {
  ActivityDetailScreen
});
})(); } catch (e) { __ds_ns.__errors.push({ path: "ui_kits/openvitals-app/ActivityDetailScreen.jsx", error: String((e && e.message) || e) }); }

// ui_kits/openvitals-app/BeverageScreen.jsx
try { (() => {
// BeverageScreen — hydration logging. Composes TopBar, Button, AccentIconChip.
const {
  TopBar,
  Button,
  AccentIconChip
} = window.OpenVitalsDesignSystem_626946;
function BeverageScreen({
  nav
}) {
  const [total, setTotal] = React.useState(0);
  const goal = 2.0;
  const cats = [['Coffees', 83], ['Energy drinks', 91], ['Teas', 16], ['Chocolate drinks', 3], ['Carbonated soft drinks', 21]];
  return /*#__PURE__*/React.createElement("div", {
    style: {
      paddingBottom: 32
    }
  }, /*#__PURE__*/React.createElement(TopBar, {
    title: "Beverage entry",
    onBack: () => nav('dashboard')
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      padding: '4px 16px 0'
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      alignItems: 'center',
      gap: 12
    }
  }, /*#__PURE__*/React.createElement(AccentIconChip, {
    icon: "local_drink",
    color: "var(--ov-metric-hydration)",
    size: 40
  }), /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("div", {
    style: {
      font: 'var(--ov-weight-semibold) var(--ov-title-md-size)/var(--ov-title-md-line) var(--ov-font-sans)',
      color: 'var(--ov-on-surface)'
    }
  }, "Log beverage"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: 'var(--ov-weight-regular) var(--ov-body-md-size)/var(--ov-body-md-line) var(--ov-font-sans)',
      color: 'var(--ov-on-surface-variant)'
    }
  }, "Saved directly to Health Connect"))), /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 14,
      background: 'var(--ov-surface-container-high)',
      borderRadius: 'var(--ov-radius-md)',
      padding: 18
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      textAlign: 'right',
      font: 'var(--ov-weight-semibold) var(--ov-headline-sm-size)/1 var(--ov-font-sans)',
      color: 'var(--ov-on-surface)',
      fontFeatureSettings: "'tnum'"
    }
  }, total.toFixed(2), " L / ", goal.toFixed(2), " L"), /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 14,
      height: 6,
      borderRadius: 999,
      background: 'var(--ov-outline-variant)',
      position: 'relative'
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      position: 'absolute',
      left: 0,
      top: 0,
      bottom: 0,
      borderRadius: 999,
      width: `${Math.min(1, total / goal) * 100}%`,
      background: 'var(--ov-secondary)'
    }
  }))), /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'space-between',
      marginTop: 18
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      font: 'var(--ov-weight-semibold) var(--ov-title-md-size)/1 var(--ov-font-sans)',
      color: 'var(--ov-on-surface)'
    }
  }, "Drink catalog"), /*#__PURE__*/React.createElement("span", {
    className: "material-symbols-outlined",
    style: {
      fontSize: 22,
      color: 'var(--ov-on-surface-variant)'
    }
  }, "edit")), /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 12,
      border: '1px solid var(--ov-outline-variant)',
      borderRadius: 'var(--ov-radius-sm)',
      padding: '14px 16px',
      color: 'var(--ov-on-surface-variant)',
      font: 'var(--ov-weight-regular) var(--ov-body-lg-size)/1 var(--ov-font-sans)'
    }
  }, "Search drinks"), /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 16,
      font: 'var(--ov-weight-medium) var(--ov-title-sm-size)/1 var(--ov-font-sans)',
      color: 'var(--ov-on-surface-variant)'
    }
  }, "Saved drinks"), /*#__PURE__*/React.createElement("div", {
    onClick: () => setTotal(t => Math.round((t + 0.35) * 100) / 100),
    style: {
      marginTop: 10,
      background: 'var(--ov-surface-container-lowest)',
      borderRadius: 'var(--ov-radius-sm)',
      padding: 16,
      display: 'flex',
      alignItems: 'center',
      gap: 14,
      cursor: 'pointer'
    }
  }, /*#__PURE__*/React.createElement(AccentIconChip, {
    icon: "local_drink",
    color: "var(--ov-metric-hydration)",
    size: 40
  }), /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("div", {
    style: {
      font: 'var(--ov-weight-semibold) var(--ov-title-md-size)/1 var(--ov-font-sans)',
      color: 'var(--ov-on-surface)'
    }
  }, "water"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: 'var(--ov-weight-medium) var(--ov-body-md-size)/1.5 var(--ov-font-sans)',
      color: 'var(--ov-on-surface-variant)'
    }
  }, "350 ml"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: 'var(--ov-weight-semibold) var(--ov-body-sm-size)/1.4 var(--ov-font-sans)',
      color: 'var(--ov-on-surface)'
    }
  }, "Liquid only"))), cats.map(([name, n]) => /*#__PURE__*/React.createElement("div", {
    key: name,
    style: {
      marginTop: 10,
      background: 'var(--ov-surface-container-high)',
      borderRadius: 'var(--ov-radius-sm)',
      padding: '16px 18px',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'space-between'
    }
  }, /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("div", {
    style: {
      font: 'var(--ov-weight-semibold) var(--ov-title-md-size)/1.3 var(--ov-font-sans)',
      color: 'var(--ov-on-surface)'
    }
  }, name), /*#__PURE__*/React.createElement("div", {
    style: {
      font: 'var(--ov-weight-regular) var(--ov-body-md-size)/1.3 var(--ov-font-sans)',
      color: 'var(--ov-on-surface-variant)'
    }
  }, n, " drinks")), /*#__PURE__*/React.createElement("span", {
    className: "material-symbols-outlined",
    style: {
      fontSize: 24,
      color: 'var(--ov-on-surface)'
    }
  }, "expand_more"))), /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 18
    }
  }, /*#__PURE__*/React.createElement(Button, {
    variant: "filled",
    icon: "add",
    fullWidth: true,
    size: "large",
    style: {
      background: 'var(--ov-secondary)',
      color: 'var(--ov-on-secondary)'
    }
  }, "New drink"))));
}
Object.assign(window, {
  BeverageScreen
});
})(); } catch (e) { __ds_ns.__errors.push({ path: "ui_kits/openvitals-app/BeverageScreen.jsx", error: String((e && e.message) || e) }); }

// ui_kits/openvitals-app/BodyEnergyScreen.jsx
try { (() => {
// BodyEnergyScreen — recovery detail with the line chart + data confidence.
const {
  TopBar,
  DateNavigator,
  Card,
  MetricLineChart,
  DataConfidenceCard
} = window.OpenVitalsDesignSystem_626946;
function Stat({
  label,
  value
}) {
  return /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("div", {
    style: {
      font: 'var(--ov-weight-medium) var(--ov-label-md-size)/1.3 var(--ov-font-sans)',
      color: 'var(--ov-on-surface-variant)'
    }
  }, label), /*#__PURE__*/React.createElement("div", {
    style: {
      font: 'var(--ov-weight-bold) var(--ov-title-lg-size)/1.1 var(--ov-font-sans)',
      color: 'var(--ov-on-surface)'
    }
  }, value));
}
function BodyEnergyScreen({
  nav
}) {
  const accent = 'var(--ov-metric-workout)';
  return /*#__PURE__*/React.createElement("div", {
    style: {
      paddingBottom: 32
    }
  }, /*#__PURE__*/React.createElement(TopBar, {
    title: "Body Energy",
    onBack: () => nav('dashboard')
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      padding: '4px 16px 0'
    }
  }, /*#__PURE__*/React.createElement(DateNavigator, {
    title: "Today",
    subtitle: "Sat 4 Jul",
    canGoForward: false,
    onPrevious: () => {},
    onOpenCalendar: () => {}
  })), /*#__PURE__*/React.createElement("div", {
    style: {
      padding: '16px 16px 0'
    }
  }, /*#__PURE__*/React.createElement(Card, {
    variant: "accent",
    accentColor: accent,
    padding: 18
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      alignItems: 'flex-start',
      justifyContent: 'space-between'
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      alignItems: 'center',
      gap: 12
    }
  }, /*#__PURE__*/React.createElement("span", {
    className: "material-symbols-outlined",
    style: {
      fontSize: 28,
      color: accent
    }
  }, "battery_charging_full"), /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("div", {
    style: {
      font: 'var(--ov-weight-semibold) var(--ov-title-md-size)/1.2 var(--ov-font-sans)',
      color: 'var(--ov-on-surface)'
    }
  }, "Body Energy"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: 'var(--ov-weight-regular) var(--ov-body-md-size)/1.3 var(--ov-font-sans)',
      color: 'var(--ov-on-surface-variant)'
    }
  }, "Estimated by OpenVitals"))), /*#__PURE__*/React.createElement("div", {
    style: {
      font: 'var(--ov-weight-bold) var(--ov-headline-lg-size)/1 var(--ov-font-sans)',
      color: accent
    }
  }, "83")), /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      justifyContent: 'space-between',
      marginTop: 18
    }
  }, /*#__PURE__*/React.createElement(Stat, {
    label: "Start",
    value: "81"
  }), /*#__PURE__*/React.createElement(Stat, {
    label: "Charged",
    value: "+2"
  }), /*#__PURE__*/React.createElement(Stat, {
    label: "Drained",
    value: "-0"
  }))), /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 12
    }
  }, /*#__PURE__*/React.createElement(Card, {
    padding: 16
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: 'var(--ov-weight-bold) var(--ov-title-md-size)/1.3 var(--ov-font-sans)',
      color: 'var(--ov-on-surface)',
      marginBottom: 12
    }
  }, "Daily timeline"), /*#__PURE__*/React.createElement(MetricLineChart, {
    data: [81, 81, 82, 82, 83, 83, 83, 84],
    yTicks: [0, 50, 100],
    accentColor: accent,
    height: 180
  }))), /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 12
    }
  }, /*#__PURE__*/React.createElement(DataConfidenceCard, {
    level: "low",
    coverage: "Some timeline buckets have sparse Health Connect data",
    samples: "Estimated where calibration is incomplete",
    accentColor: accent,
    warnings: ["Charge and drain rates are approximate today"]
  }))));
}
Object.assign(window, {
  BodyEnergyScreen
});
})(); } catch (e) { __ds_ns.__errors.push({ path: "ui_kits/openvitals-app/BodyEnergyScreen.jsx", error: String((e && e.message) || e) }); }

// ui_kits/openvitals-app/DailyReadinessScreen.jsx
try { (() => {
// DailyReadinessScreen — recovery detail. Composes TopBar, DateNavigator,
// ReadinessBanner, Card, AccentIconChip from the bundle.
const {
  TopBar,
  DateNavigator,
  ReadinessBanner,
  Card,
  AccentIconChip,
  CrossMetricInsightCard
} = window.OpenVitalsDesignSystem_626946;
function SubTile({
  icon,
  color,
  label,
  value,
  muted
}) {
  return /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1,
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'space-between',
      gap: 8,
      padding: '12px 14px',
      borderRadius: 'var(--ov-radius-sm)',
      background: muted ? 'var(--ov-surface-container-high)' : `color-mix(in srgb, ${color} 16%, var(--ov-surface-container))`
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      alignItems: 'center',
      gap: 10,
      minWidth: 0
    }
  }, /*#__PURE__*/React.createElement("span", {
    className: "material-symbols-outlined",
    style: {
      fontSize: 22,
      color
    }
  }, icon), /*#__PURE__*/React.createElement("div", {
    style: {
      minWidth: 0
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: 'var(--ov-weight-medium) var(--ov-label-md-size)/var(--ov-label-md-line) var(--ov-font-sans)',
      color: 'var(--ov-on-surface-variant)'
    }
  }, label), /*#__PURE__*/React.createElement("div", {
    style: {
      font: 'var(--ov-weight-semibold) var(--ov-title-sm-size)/var(--ov-title-sm-line) var(--ov-font-sans)',
      color: 'var(--ov-on-surface)'
    }
  }, value))), /*#__PURE__*/React.createElement("span", {
    className: "material-symbols-outlined",
    style: {
      fontSize: 20,
      color: 'var(--ov-on-surface-variant)'
    }
  }, "chevron_right"));
}
function InsightBlock({
  title,
  body
}) {
  return /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 16
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: 'var(--ov-weight-semibold) var(--ov-title-sm-size)/var(--ov-title-sm-line) var(--ov-font-sans)',
      color: 'var(--ov-on-surface)'
    }
  }, title), /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 2,
      font: 'var(--ov-weight-regular) var(--ov-body-lg-size)/var(--ov-body-lg-line) var(--ov-font-sans)',
      color: 'var(--ov-on-surface)'
    }
  }, body));
}
function RecommendRow({
  icon,
  color,
  label,
  body
}) {
  return /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      gap: 12,
      alignItems: 'flex-start',
      marginTop: 14
    }
  }, /*#__PURE__*/React.createElement(AccentIconChip, {
    icon: icon,
    color: color,
    size: 36
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: 'var(--ov-weight-semibold) var(--ov-title-sm-size)/var(--ov-title-sm-line) var(--ov-font-sans)',
      color: 'var(--ov-on-surface)'
    }
  }, label), /*#__PURE__*/React.createElement("div", {
    style: {
      font: 'var(--ov-weight-regular) var(--ov-body-lg-size)/var(--ov-body-lg-line) var(--ov-font-sans)',
      color: 'var(--ov-on-surface)'
    }
  }, body)));
}
function DailyReadinessScreen({
  nav
}) {
  return /*#__PURE__*/React.createElement("div", {
    style: {
      paddingBottom: 32
    }
  }, /*#__PURE__*/React.createElement(TopBar, {
    title: "Daily Readiness",
    onBack: () => nav('dashboard')
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      padding: '4px 16px 0'
    }
  }, /*#__PURE__*/React.createElement(DateNavigator, {
    title: "Today",
    subtitle: "4 Jul 2026",
    canGoForward: false,
    onPrevious: () => {},
    onOpenCalendar: () => {}
  })), /*#__PURE__*/React.createElement("div", {
    style: {
      padding: '16px 16px 0'
    }
  }, /*#__PURE__*/React.createElement(ReadinessBanner, {
    score: "65/100",
    scoreLabel: "Readiness",
    confidence: "Medium confidence \xB7 sleep data missing",
    headline: "Train, but keep it controlled",
    body: "Do moderate training today, but avoid maximal effort. Your signals suggest this mainly because hydration is 0% of today's goal and resting heart rate is 2 bpm below your usual baseline."
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      gap: 12
    }
  }, /*#__PURE__*/React.createElement(SubTile, {
    icon: "favorite",
    color: "var(--ov-metric-heart)",
    label: "Body Energy",
    value: "63/100"
  }), /*#__PURE__*/React.createElement(SubTile, {
    icon: "fitness_center",
    color: "var(--ov-metric-workout)",
    label: "Training Readiness",
    value: "75/100",
    muted: true
  })), /*#__PURE__*/React.createElement(InsightBlock, {
    title: "HRV Status",
    body: "Needs more HRV \xB7 HRV was not available for this day."
  }), /*#__PURE__*/React.createElement(InsightBlock, {
    title: "Intensity Minutes",
    body: "Goal met \xB7 244/150 moderate-equivalent min this week; vigorous minutes count double."
  }), /*#__PURE__*/React.createElement(InsightBlock, {
    title: "Stress Level",
    body: "Low \xB7 32/100 \xB7 Signals suggest low physiological stress."
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      height: 1,
      background: 'var(--ov-outline-variant)',
      opacity: 0.5,
      margin: '18px 0 4px'
    }
  }), /*#__PURE__*/React.createElement(RecommendRow, {
    icon: "directions_run",
    color: "var(--ov-metric-heart)",
    label: "Recommended",
    body: "Zone 2 cardio, moderate strength, technique work, or an easy bike ride."
  }), /*#__PURE__*/React.createElement(RecommendRow, {
    icon: "close",
    color: "var(--ov-error)",
    label: "Avoid",
    body: "Max effort, HIIT, and very long sessions."
  }), /*#__PURE__*/React.createElement(RecommendRow, {
    icon: "self_improvement",
    color: "var(--ov-metric-mindfulness)",
    label: "Alternative",
    body: "If you feel tired, choose a 30 min easy walk or a mobility session."
  })), CrossMetricInsightCard ? /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 12
    }
  }, /*#__PURE__*/React.createElement(CrossMetricInsightCard, {
    title: "Sleep and readiness",
    direction: "positive",
    correlation: 62,
    message: "On nights you sleep longer, your next-day readiness tends to be higher.",
    pairedDays: 21,
    accentColor: "var(--ov-metric-sleep)"
  })) : null));
}
Object.assign(window, {
  DailyReadinessScreen
});
})(); } catch (e) { __ds_ns.__errors.push({ path: "ui_kits/openvitals-app/DailyReadinessScreen.jsx", error: String((e && e.message) || e) }); }

// ui_kits/openvitals-app/DashboardScreen.jsx
try { (() => {
// DashboardScreen — OpenVitals home. Composes SummaryRingCard, MetricStatCard,
// Button, TopBar, DateNavigator, SectionHeader from the design system bundle.
const {
  TopBar,
  DateNavigator,
  SummaryRingCard,
  MetricStatCard,
  Button,
  SectionHeader,
  AccentIconChip,
  SensorStatusCard
} = window.OpenVitalsDesignSystem_626946;
function DashboardScreen({
  nav
}) {
  const dots = [0, 1, 2, 3, 4, 5];
  return /*#__PURE__*/React.createElement("div", {
    style: {
      paddingBottom: 24
    }
  }, /*#__PURE__*/React.createElement(TopBar, {
    large: true,
    title: "OpenVitals",
    actions: [{
      icon: 'self_improvement',
      label: 'Mindfulness',
      onClick: () => {}
    }, {
      icon: 'workspace_premium',
      label: 'Achievements',
      onClick: () => nav('achievements')
    }, {
      icon: 'settings',
      label: 'Settings',
      onClick: () => nav('settings')
    }]
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      padding: '4px 16px 0'
    }
  }, /*#__PURE__*/React.createElement(DateNavigator, {
    title: "Sun, 28 Jun",
    subtitle: "28 Jun 2026",
    canGoForward: false,
    onPrevious: () => {},
    onNext: () => {},
    onOpenCalendar: () => {}
  })), /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'grid',
      gridTemplateColumns: '1fr 1fr',
      gap: 12,
      padding: '16px 16px 0'
    }
  }, /*#__PURE__*/React.createElement(SummaryRingCard, {
    title: "Steps",
    value: "19,576",
    subtitle: "steps of 8,000",
    progress: 1,
    accentColor: "var(--ov-metric-steps)",
    size: 150,
    style: {
      aspectRatio: '1 / 1'
    },
    onClick: () => nav('activity')
  }), /*#__PURE__*/React.createElement(SummaryRingCard, {
    title: "Weekly cardio",
    value: "100%",
    subtitle: "802 of 25",
    progress: 1,
    accentColor: "var(--ov-metric-workout)",
    size: 150,
    style: {
      aspectRatio: '1 / 1'
    }
  })), /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      gap: 12,
      padding: '14px 16px 0',
      alignItems: 'center'
    }
  }, /*#__PURE__*/React.createElement(Button, {
    variant: "tonal",
    icon: "add",
    fullWidth: true,
    size: "large",
    onClick: () => nav('beverage')
  }, "Log"), /*#__PURE__*/React.createElement(Button, {
    variant: "filled",
    icon: "directions_run",
    fullWidth: true,
    size: "large",
    onClick: () => nav('recording')
  }, "Start"), /*#__PURE__*/React.createElement("button", {
    "aria-label": "Edit dashboard",
    style: {
      width: 44,
      height: 44,
      flex: '0 0 auto',
      borderRadius: 999,
      border: 'none',
      background: 'transparent',
      color: 'var(--ov-on-surface-variant)',
      cursor: 'pointer'
    }
  }, /*#__PURE__*/React.createElement("span", {
    className: "material-symbols-outlined",
    style: {
      fontSize: 20
    }
  }, "edit"))), /*#__PURE__*/React.createElement("div", {
    style: {
      height: 1,
      background: 'var(--ov-outline-variant)',
      opacity: 0.6,
      margin: '16px 16px 4px'
    }
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'grid',
      gridTemplateColumns: '1fr 1fr',
      gap: 12,
      padding: '12px 16px 0'
    }
  }, /*#__PURE__*/React.createElement(MetricStatCard, {
    title: "Distance",
    value: "18.6",
    unit: "km",
    icon: "straighten",
    accentColor: "var(--ov-metric-distance)",
    progress: 1,
    onClick: () => nav('activity')
  }), /*#__PURE__*/React.createElement(MetricStatCard, {
    title: "Total calories",
    value: "3,178",
    unit: "kcal",
    icon: "local_fire_department",
    accentColor: "var(--ov-metric-calories)",
    progress: 0.9
  }), /*#__PURE__*/React.createElement(MetricStatCard, {
    title: "Active calories",
    value: "837",
    unit: "kcal",
    icon: "mode_heat",
    accentColor: "var(--ov-metric-active-calories)",
    progress: 0.7
  }), /*#__PURE__*/React.createElement(MetricStatCard, {
    title: "Elevation",
    value: "84",
    unit: "m",
    icon: "landscape",
    accentColor: "var(--ov-metric-elevation)",
    progress: 0.5
  }), /*#__PURE__*/React.createElement(MetricStatCard, {
    title: "Sleep",
    value: "5h 15m",
    subtitle: "74 \xB7 Fair",
    icon: "bedtime",
    accentColor: "var(--ov-metric-sleep)",
    progress: 0.5,
    onClick: () => nav('readiness')
  }), /*#__PURE__*/React.createElement(MetricStatCard, {
    title: "Beverages",
    value: "0.00",
    unit: "L",
    icon: "local_drink",
    accentColor: "var(--ov-metric-hydration)",
    progress: 0,
    onClick: () => nav('beverage')
  })), /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      justifyContent: 'center',
      gap: 8,
      padding: '18px 0 4px'
    }
  }, dots.map(d => /*#__PURE__*/React.createElement("span", {
    key: d,
    style: {
      width: 8,
      height: 8,
      borderRadius: 999,
      background: d === 0 ? 'var(--ov-primary)' : 'var(--ov-outline-variant)'
    }
  }))), /*#__PURE__*/React.createElement("div", {
    style: {
      padding: '8px 16px 0'
    }
  }, SensorStatusCard ? /*#__PURE__*/React.createElement(SensorStatusCard, {
    batteryPercent: 64,
    activeCount: 2,
    connectedCount: 1,
    onClick: () => nav('settings')
  }) : null), /*#__PURE__*/React.createElement("div", {
    style: {
      padding: '8px 16px 0'
    }
  }, /*#__PURE__*/React.createElement(SectionHeader, {
    text: "Activities",
    trailing: "chevron_right",
    onTrailingClick: () => nav('activity')
  }), /*#__PURE__*/React.createElement("div", {
    onClick: () => nav('activity'),
    style: {
      background: 'var(--ov-surface-container)',
      borderRadius: 'var(--ov-radius-md)',
      padding: 16,
      cursor: 'pointer'
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'space-between'
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      alignItems: 'center',
      gap: 12
    }
  }, /*#__PURE__*/React.createElement(AccentIconChip, {
    icon: "directions_walk",
    color: "var(--ov-metric-workout)"
  }), /*#__PURE__*/React.createElement("span", {
    style: {
      font: 'var(--ov-weight-semibold) var(--ov-title-md-size)/var(--ov-title-md-line) var(--ov-font-sans)',
      color: 'var(--ov-on-surface)'
    }
  }, "Workout")), /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'inline-flex',
      alignItems: 'center',
      gap: 8,
      padding: '6px 12px',
      borderRadius: 999,
      border: '1px solid var(--ov-outline-variant)'
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      width: 14,
      height: 14,
      borderRadius: '50%',
      background: 'var(--ov-outline)'
    }
  }), /*#__PURE__*/React.createElement("span", {
    style: {
      font: 'var(--ov-weight-medium) var(--ov-label-md-size)/1 var(--ov-font-sans)',
      color: 'var(--ov-on-surface-variant)'
    }
  }, "Gadgetbridge"))), /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 12,
      font: 'var(--ov-weight-regular) var(--ov-body-md-size)/var(--ov-body-md-line) var(--ov-font-sans)',
      color: 'var(--ov-on-surface-variant)'
    }
  }, "Walking"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: 'var(--ov-weight-bold) var(--ov-headline-lg-size)/var(--ov-headline-lg-line) var(--ov-font-sans)',
      color: 'var(--ov-on-surface)'
    }
  }, "2h 46m"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: 'var(--ov-weight-regular) var(--ov-body-sm-size)/var(--ov-body-sm-line) var(--ov-font-sans)',
      color: 'var(--ov-on-surface-variant)'
    }
  }, "10:33 AM"))));
}
Object.assign(window, {
  DashboardScreen
});
})(); } catch (e) { __ds_ns.__errors.push({ path: "ui_kits/openvitals-app/DashboardScreen.jsx", error: String((e && e.message) || e) }); }

// ui_kits/openvitals-app/DisplaySettingsScreen.jsx
try { (() => {
// DisplaySettingsScreen — a Settings sub-screen showcasing the form controls.
const {
  TopBar,
  Select,
  RadioGroup,
  Switch,
  SectionHeader
} = window.OpenVitalsDesignSystem_626946;
function Group({
  children
}) {
  return /*#__PURE__*/React.createElement("div", {
    style: {
      background: 'var(--ov-surface-container)',
      borderRadius: 'var(--ov-radius-md)',
      padding: 16
    }
  }, children);
}
function DisplaySettingsScreen({
  nav
}) {
  const [lang, setLang] = React.useState('System default');
  const [units, setUnits] = React.useState('Imperial');
  const [theme, setTheme] = React.useState('Dark');
  const [dynamic, setDynamic] = React.useState(true);
  const [amoled, setAmoled] = React.useState(false);
  return /*#__PURE__*/React.createElement("div", {
    style: {
      paddingBottom: 32
    }
  }, /*#__PURE__*/React.createElement(TopBar, {
    title: "Display",
    onBack: () => nav('settings')
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      flexDirection: 'column',
      gap: 12,
      padding: '8px 16px 0'
    }
  }, /*#__PURE__*/React.createElement(Group, null, /*#__PURE__*/React.createElement(Select, {
    label: "App language",
    value: lang,
    onChange: setLang,
    options: ['System default', 'English', 'Español', 'Deutsch', 'Italiano']
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      height: 14
    }
  }), /*#__PURE__*/React.createElement(Select, {
    label: "Units",
    value: units,
    onChange: setUnits,
    options: ['Metric', 'Imperial']
  })), /*#__PURE__*/React.createElement(SectionHeader, {
    text: "Theme"
  }), /*#__PURE__*/React.createElement(Group, null, /*#__PURE__*/React.createElement(RadioGroup, {
    value: theme,
    onChange: setTheme,
    options: ['System', 'Light', 'Dark', 'AMOLED']
  })), /*#__PURE__*/React.createElement(SectionHeader, {
    text: "Options"
  }), /*#__PURE__*/React.createElement(Group, null, /*#__PURE__*/React.createElement(Switch, {
    label: "Dynamic color (Material You)",
    checked: dynamic,
    onChange: setDynamic
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      height: 4
    }
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      height: 1,
      background: 'var(--ov-outline-variant)',
      opacity: 0.5,
      margin: '10px 0'
    }
  }), /*#__PURE__*/React.createElement(Switch, {
    label: "Pure black in dark mode",
    checked: amoled,
    onChange: setAmoled
  }))));
}
Object.assign(window, {
  DisplaySettingsScreen
});
})(); } catch (e) { __ds_ns.__errors.push({ path: "ui_kits/openvitals-app/DisplaySettingsScreen.jsx", error: String((e && e.message) || e) }); }

// ui_kits/openvitals-app/RecordingScreen.jsx
try { (() => {
// RecordingScreen — live activity recording (Stats tab). Composes TopBar.
const {
  TopBar
} = window.OpenVitalsDesignSystem_626946;
function RecordingScreen({
  nav
}) {
  const tabs = ['Map', 'Stats', 'Intervals', 'By time', 'By distance'];
  const stats = [['—', 'CADENCE', 'rpm'], ['0.0', 'SPEED', 'mph'], ['0', 'DISTANCE', 'ft'], ['0:15', 'TOTAL TIME', '']];
  const accent = 'var(--ov-recording-light-accent)';
  return /*#__PURE__*/React.createElement("div", {
    style: {
      minHeight: '100%',
      background: 'var(--ov-surface-container-lowest)'
    }
  }, /*#__PURE__*/React.createElement(TopBar, {
    title: "Recording activity",
    onBack: () => nav('dashboard'),
    actions: [{
      icon: 'wb_sunny',
      label: 'Outdoor mode'
    }]
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      gap: 20,
      padding: '0 16px',
      borderBottom: '1px solid var(--ov-outline-variant)',
      overflowX: 'auto'
    }
  }, tabs.map((t, i) => /*#__PURE__*/React.createElement("div", {
    key: t,
    style: {
      padding: '12px 0',
      whiteSpace: 'nowrap',
      font: `${i === 1 ? 'var(--ov-weight-bold)' : 'var(--ov-weight-medium)'} var(--ov-title-md-size)/1 var(--ov-font-sans)`,
      color: accent,
      borderBottom: i === 1 ? `3px solid ${accent}` : '3px solid transparent'
    }
  }, t))), /*#__PURE__*/React.createElement("div", {
    style: {
      padding: '28px 20px 8px'
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      width: 44,
      height: 5,
      borderRadius: 3,
      background: accent,
      marginBottom: 10
    }
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      alignItems: 'baseline',
      justifyContent: 'space-between'
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      font: 'var(--ov-weight-bold) var(--ov-title-sm-size)/1 var(--ov-font-sans)',
      letterSpacing: '.5px',
      color: accent
    }
  }, "HEART RATE"), /*#__PURE__*/React.createElement("span", {
    style: {
      font: 'var(--ov-weight-regular) var(--ov-title-lg-size)/1 var(--ov-font-sans)',
      color: 'var(--ov-on-surface)'
    }
  }, "\u2014 bpm"))), /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'grid',
      gridTemplateColumns: '1fr 1fr',
      gap: 24,
      padding: '24px 20px'
    }
  }, stats.map(([v, l, u]) => /*#__PURE__*/React.createElement("div", {
    key: l
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      alignItems: 'baseline',
      gap: 6
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      font: 'var(--ov-weight-bold) var(--ov-headline-lg-size)/1 var(--ov-font-sans)',
      color: 'var(--ov-on-surface)',
      fontFeatureSettings: "'tnum'"
    }
  }, v), u ? /*#__PURE__*/React.createElement("span", {
    style: {
      font: 'var(--ov-weight-regular) var(--ov-body-md-size)/1 var(--ov-font-sans)',
      color: 'var(--ov-on-surface-variant)'
    }
  }, u) : null), /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 4,
      font: 'var(--ov-weight-medium) var(--ov-title-sm-size)/1 var(--ov-font-sans)',
      letterSpacing: '.5px',
      color: accent
    }
  }, l)))), /*#__PURE__*/React.createElement("div", {
    style: {
      padding: '0 20px',
      font: 'var(--ov-weight-regular) var(--ov-body-md-size)/1 var(--ov-font-sans)',
      color: 'var(--ov-on-surface-variant)'
    }
  }, "Last accuracy 13 ft"), /*#__PURE__*/React.createElement("div", {
    style: {
      position: 'sticky',
      bottom: 0,
      marginTop: 32,
      padding: '16px 20px 20px',
      background: 'var(--ov-surface-container-lowest)',
      borderTop: '1px solid var(--ov-outline-variant)'
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      justifyContent: 'space-between'
    }
  }, [['pause', 'Pause'], ['crop_free', 'Focus'], ['check', 'Finish']].map(([ic, lb]) => /*#__PURE__*/React.createElement("button", {
    key: lb,
    onClick: () => {
      if (lb === 'Finish') nav('activity');
    },
    style: {
      display: 'flex',
      flexDirection: 'column',
      alignItems: 'center',
      gap: 4,
      border: 'none',
      background: 'transparent',
      color: 'var(--ov-on-surface)',
      cursor: 'pointer',
      font: 'var(--ov-weight-medium) var(--ov-title-md-size)/1 var(--ov-font-sans)'
    }
  }, /*#__PURE__*/React.createElement("span", {
    className: "material-symbols-outlined",
    style: {
      fontSize: 22
    }
  }, ic), lb)))));
}
Object.assign(window, {
  RecordingScreen
});
})(); } catch (e) { __ds_ns.__errors.push({ path: "ui_kits/openvitals-app/RecordingScreen.jsx", error: String((e && e.message) || e) }); }

// ui_kits/openvitals-app/SettingsScreen.jsx
try { (() => {
// SettingsScreen — settings list. Composes TopBar, SettingsListItem, SectionHeader.
const {
  TopBar,
  SettingsListItem,
  SectionHeader
} = window.OpenVitalsDesignSystem_626946;
function SettingsScreen({
  nav
}) {
  const items = [['tune', 'Display', 'Language, units, and theme'], ['directions_run', 'Activities', 'Activity week, favorite activity, recording, and offline maps'], ['bluetooth', 'Sensors & devices', 'Heart rate, cadence, and power sensors'], ['restaurant', 'Nutrition', 'Calories data and caffeine personalization'], ['favorite', 'Recovery', 'Sleep range and Body Energy calibration'], ['folder_open', 'Data Import', 'Import Apple Health export records with Health Connect equivalents'], ['health_and_safety', 'Health Connect', 'Sync, permissions, access, and app lock'], ['bug_report', 'Debug diagnostics', 'Save sanitized diagnostics logs for troubleshooting']];
  return /*#__PURE__*/React.createElement("div", {
    style: {
      paddingBottom: 32
    }
  }, /*#__PURE__*/React.createElement(TopBar, {
    title: "Settings",
    onBack: () => nav('dashboard')
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      flexDirection: 'column',
      gap: 8,
      padding: '8px 16px 0'
    }
  }, items.map(([icon, title, sub], i) => /*#__PURE__*/React.createElement(SettingsListItem, {
    key: i,
    icon: icon,
    title: title,
    supportingText: sub,
    onClick: () => {
      if (title === 'Display') nav('display');
      if (title === 'Recovery') nav('readiness');
      if (title === 'Nutrition') nav('beverage');
    }
  })), /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 8
    }
  }, /*#__PURE__*/React.createElement(SectionHeader, {
    text: "Support"
  })), /*#__PURE__*/React.createElement(SettingsListItem, {
    icon: "volunteer_activism",
    title: "Support OpenVitals",
    supportingText: "Report bugs, join community support discussions, or help fund ongoing development."
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      gap: 8,
      padding: '16px 0 4px'
    }
  }, /*#__PURE__*/React.createElement("span", {
    className: "material-symbols-outlined",
    style: {
      fontSize: 20,
      color: 'var(--ov-on-surface)'
    }
  }, "open_in_new"), /*#__PURE__*/React.createElement("span", {
    style: {
      font: 'var(--ov-weight-semibold) var(--ov-title-md-size)/1 var(--ov-font-sans)',
      color: 'var(--ov-on-surface)',
      textDecoration: 'underline'
    }
  }, "Report an issue"))));
}
Object.assign(window, {
  SettingsScreen
});
})(); } catch (e) { __ds_ns.__errors.push({ path: "ui_kits/openvitals-app/SettingsScreen.jsx", error: String((e && e.message) || e) }); }

__ds_ns.Button = __ds_scope.Button;

__ds_ns.IconButton = __ds_scope.IconButton;

__ds_ns.Card = __ds_scope.Card;

__ds_ns.MetricCard = __ds_scope.MetricCard;

__ds_ns.MetricStatCard = __ds_scope.MetricStatCard;

__ds_ns.SummaryRingCard = __ds_scope.SummaryRingCard;

__ds_ns.MetricBarChart = __ds_scope.MetricBarChart;

__ds_ns.MetricLineChart = __ds_scope.MetricLineChart;

__ds_ns.PeriodHeatmap = __ds_scope.PeriodHeatmap;

__ds_ns.Sparkline = __ds_scope.Sparkline;

__ds_ns.AccentIconChip = __ds_scope.AccentIconChip;

__ds_ns.DetailRow = __ds_scope.DetailRow;

__ds_ns.Icon = __ds_scope.Icon;

__ds_ns.ReadinessBanner = __ds_scope.ReadinessBanner;

__ds_ns.SettingsListItem = __ds_scope.SettingsListItem;

__ds_ns.Checkbox = __ds_scope.Checkbox;

__ds_ns.RadioGroup = __ds_scope.RadioGroup;

__ds_ns.Select = __ds_scope.Select;

__ds_ns.Slider = __ds_scope.Slider;

__ds_ns.Switch = __ds_scope.Switch;

__ds_ns.TextField = __ds_scope.TextField;

__ds_ns.AchievementBadge = __ds_scope.AchievementBadge;

__ds_ns.CrossMetricInsightCard = __ds_scope.CrossMetricInsightCard;

__ds_ns.DataConfidenceCard = __ds_scope.DataConfidenceCard;

__ds_ns.SensorStatusCard = __ds_scope.SensorStatusCard;

__ds_ns.BottomNavBar = __ds_scope.BottomNavBar;

__ds_ns.DateNavigator = __ds_scope.DateNavigator;

__ds_ns.SectionHeader = __ds_scope.SectionHeader;

__ds_ns.TimeRangeSelector = __ds_scope.TimeRangeSelector;

__ds_ns.TopBar = __ds_scope.TopBar;

})();

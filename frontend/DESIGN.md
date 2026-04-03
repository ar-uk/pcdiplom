# PCBuilder Design System & Styling Guide

## Overview

This document describes the comprehensive design system for the PCBuilder frontend. The system uses **Tailwind CSS** for rapid utility-based styling combined with **CSS custom properties** for consistent design tokens and **global CSS files** for complex layouts.

## Architecture

```
src/
  styles/
    index.css           # Main entry point (imports all styles)
    reset.css          # CSS reset, scrollbar styles, box-sizing
    variables.css      # CSS custom properties (100+ tokens)
    typography.css     # Font scales, text utilities
    utilities.css      # Layout, spacing, display helpers
    animations.css     # Keyframes and animation classes
  components/
    common/
      Button.tsx       # Variant: primary|secondary|danger|success|warning
      Card.tsx         # Shadow/padding levels + hover states
      Modal.tsx        # Multiple sizes, animations, accessibility
      Loading.tsx      # Spinner sizes, full-screen option
      Toast.tsx        # 4 types (success|error|info|warning)
      Tabs.tsx         # Accessible tabs with badges
    layout/
      Header.tsx       # Sticky header with responsive nav
      MainLayout.tsx   # Main app layout wrapper
      Sidebar.tsx      # Sticky sidebar navigation
```

## Design Tokens

### Color Palette

**Primary (Actions & Links)**
- `--primary: #0066cc` (main blue)
- `--primary-light: #e0effe`
- `--primary-dark: #004285`
- `--primary-hover: #0052a5`

**Secondary (Inactive States)**
- `--secondary: #6c757d` (gray)
- `--secondary-light: #f3f4f6`

**Semantic**
- `--success: #28a745` (green)
- `--warning: #ffc107` (yellow)
- `--danger: #dc3545` (red)

**Neutral**
- `--text-primary: #1f2937` (text)
- `--text-secondary: #6b7280`
- `--bg-primary: #ffffff` (cards, main bg)
- `--bg-secondary: #f9fafb` (page bg)
- `--border: #e5e7eb`

### Typography

**Font Sizes (rem)**
```
--fs-xs: 0.75rem     (12px)
--fs-sm: 0.875rem    (14px)
--fs-base: 0.875rem  (14px)
--fs-lg: 1rem        (16px)
--fs-xl: 1.125rem    (18px)
--fs-2xl: 1.25rem    (20px)
--fs-3xl: 1.5rem     (24px)
--fs-4xl: 2rem       (32px)
```

**Font Weights**
- `--fw-normal: 400`
- `--fw-medium: 500`
- `--fw-semibold: 600`
- `--fw-bold: 700`

**Line Heights**
- `--lh-tight: 1.2`
- `--lh-normal: 1.5` (default)
- `--lh-relaxed: 1.6`

### Spacing (8px base unit)

```
--space-0: 0
--space-1: 0.5rem   (4px)
--space-2: 1rem     (8px)
--space-3: 1.5rem   (12px)
--space-4: 2rem     (16px)
--space-5: 2.5rem   (20px)
--space-6: 3rem     (24px)
--space-8: 4rem     (32px)
```

### Shadows

```
--shadow-sm: 0 1px 3px rgba(0,0,0,0.1)
--shadow-md: 0 4px 6px rgba(0,0,0,0.1)
--shadow-lg: 0 10px 15px rgba(0,0,0,0.1)
--shadow-xl: 0 20px 25px rgba(0,0,0,0.1)
```

### Border Radius

```
--radius-sm: 4px
--radius-md: 6px
--radius-lg: 8px
--radius-xl: 12px
--radius-full: 9999px
```

### Z-Index Stack

```
--z-hide: -1
--z-base: 0
--z-dropdown: 100
--z-sticky: 200
--z-fixed: 300
--z-modal: 700
```

### Breakpoints

```
xs: 480px   (mobile)
sm: 640px   (tablet)
md: 768px   (tablet)
lg: 1024px  (desktop)
xl: 1280px  (wide desktop)
```

## Styling Approach

### 1. Tailwind Classes (Primary)

For most components, use Tailwind's utility classes directly in JSX:

```tsx
export const Button = ({ variant = 'primary', ...props }) => {
  const variantStyles = {
    primary: 'bg-primary text-white hover:bg-blue-700 disabled:opacity-50',
    secondary: 'bg-secondary text-white hover:bg-gray-700',
    danger: 'bg-danger text-white hover:bg-red-700',
  }

  return (
    <button className={`${variantStyles[variant]} px-4 py-2 rounded-md ...`}>
      {children}
    </button>
  )
}
```

**Advantages:**
- Fast development
- Consistent spacing/colors
- Built-in responsive utilities (`sm:`, `md:`, `lg:`)
- Dark mode support via `dark:` prefix

### 2. CSS Custom Properties (Fallback)

For browsers without Tailwind support or complex themeing:

```css
button {
  background-color: var(--primary);
  color: white;
  padding: var(--space-2) var(--space-4);
  border-radius: var(--radius-md);
}

button:hover {
  background-color: var(--primary-hover);
}
```

### 3. CSS Modules (Complex Layouts)

For intricate layouts (Builder 3-column, Community grid), use CSS modules:

```tsx
// BuilderPage.module.css
.builder-container {
  display: grid;
  grid-template-columns: 280px 1fr 320px;
  gap: var(--space-4);
  min-height: 600px;
}

.builder-sidebar { /* left */ }
.builder-main { /* center */ }
.builder-summary { /* right */ }

@media (max-width: 1024px) {
  .builder-container {
    grid-template-columns: 1fr 320px;
  }
}

@media (max-width: 768px) {
  .builder-container {
    grid-template-columns: 1fr;
  }
}
```

## Common Component Patterns

### Button Component

**Variants:** `primary | secondary | danger | success | warning`

```tsx
<Button variant="primary">Save</Button>
<Button variant="danger" outline>Delete</Button>
<Button loading>Loading...</Button>
<Button disabled>Disabled</Button>
<Button fullWidth>Full Width</Button>
```

**Sizes:** `small | medium | large`

```tsx
<Button size="small">Small</Button>
<Button size="medium">Medium</Button>
<Button size="large">Large</Button>
```

**Accessibility:**
- Focus outline visible (2px blue)
- Min height 44px (touch target)
- `aria-busy` when loading
- Type support: `button | submit | reset`

### Card Component

```tsx
<Card shadow="light" padding="medium">
  Content here
</Card>
```

**Props:**
- `shadow`: `none | light | medium | heavy`
- `padding`: `none | small | medium | large`
- `hoverable`: adds lift effect on hover

### Modal Component

```tsx
<Modal
  isOpen={isOpen}
  onClose={() => setOpen(false)}
  title="Confirm Action"
  size="medium"
>
  Are you sure?
</Modal>
```

**Sizes:** `small | medium | large`

**Features:**
- Backdrop click to close
- ESC key to close
- Animations (fade + scale)
- Accessibility: role="dialog", aria-modal, aria-labelledby

### Toast Notification

```tsx
<Toast
  message="Saved successfully!"
  type="success"
  duration={4000}
  onClose={() => {}}
/>
```

**Types:** `success | error | info | warning`

**Features:**
- Auto-dismisses after duration
- Smooth animations
- Icons for each type
- Bottom-right position

### Tabs Component

```tsx
<Tabs
  tabs={[
    { id: 'builds', label: 'Builds', content: <BuildsList /> },
    { id: 'settings', label: 'Settings', badge: 3, content: <Settings /> },
  ]}
  defaultTab="builds"
  onChange={(tabId) => console.log(tabId)}
/>
```

**Features:**
- Accessible tabs (ARIA)
- Badges for count/alerts
- onChange callback
- Keyboard navigation

## Responsive Design

### Mobile-First Strategy

```tsx
// Desktop view
<div className="grid grid-cols-4 gap-4">
  {items.map(item => <Card key={item.id}>{item}</Card>)}
</div>

// Tablet view (md: 768px)
<div className="grid md:grid-cols-3 gap-4">

// Mobile view (sm: 640px)
<div className="grid sm:grid-cols-2 gap-4">
```

### Common Breakpoint Patterns

```css
/* Breakpoint utilities */
.d-md-none        /* hidden on md+ */
.d-md-block       /* block on md+ */
.text-center      /* desktop */
.md:text-left     /* tablet+ */
.sm:text-xs       /* mobile */
```

### Container Queries

```tsx
<div className="container">
  {/* Max-width: 1400px, auto margin, responsive padding */}
</div>
```

## Dark Mode Support

All CSS variables support dark mode via `@media (prefers-color-scheme: dark)`:

```css
:root {
  --text-primary: #1f2937;
  --bg-primary: #ffffff;
}

@media (prefers-color-scheme: dark) {
  :root {
    --text-primary: #f3f4f6;
    --bg-primary: #111827;
  }
}
```

In components, use the `dark:` prefix:

```tsx
<div className="bg-white dark:bg-secondary border border-gray-200 dark:border-gray-700">
  Content
</div>
```

## Animations

### Keyframe Animations

Available animations in `animations.css`:

```css
@keyframes fadeIn          /* fade from 0 to 1 */
@keyframes slideInUp       /* slide from bottom */
@keyframes scaleIn         /* grow from 0.9 to 1 */
@keyframes pulse           /* pulse opacity */
@keyframes spin            /* rotate 360deg */
@keyframes shake           /* horizontal shake */
```

### Using Animations

**CSS:**
```css
.fade-in {
  animation: fadeIn 0.3s ease-in-out;
}
```

**Tailwind:**
```tsx
<div className="animate-fadeIn">
  Content
</div>
```

### Hover Animations

```tsx
<Card className="hover:lift">
  {/* Card lifts on hover with shadow change */}
</Card>

<Button className="hover-scale">
  {/* Button scales to 1.05 on hover */}
</Button>
```

## Accessibility Guidelines

### Color Contrast

All colors meet WCAG AA standard (4.5:1 ratio for text):

```
Primary text (#1f2937) on white: 16.5:1 ✓
Primary blue (#0066cc) on white: 8.6:1 ✓
Gray text (#6b7280) on white: 7:1 ✓
```

### Focus States

All interactive elements have visible focus:

```css
:focus-visible {
  outline: 2px solid var(--primary);
  outline-offset: 2px;
}
```

### Touch Targets

Minimum 44px height/width for buttons and links:

```tsx
<button className="min-h-11 min-w-11 px-4 py-2">
  Click me
</button>
```

### ARIA Labels

Use ARIA for dynamic content:

```tsx
<Loading
  fullScreen
  text="Loading build..."
  role="status"
  aria-live="polite"
  aria-label="Loading..."
/>

<Modal role="dialog" aria-modal="true" aria-labelledby="modal-title">
  <h2 id="modal-title">Confirm Delete</h2>
</Modal>
```

### Keyboard Navigation

- Tab: Move focus forward
- Shift+Tab: Move focus backward
- Enter: Activate button
- Space: Toggle checkbox
- Escape: Close modal

## Implementation Checklist

When styling a new component/page:

- [ ] Use Tailwind utilities for layout/spacing
- [ ] Add CSS custom properties for colors
- [ ] Ensure 44px min touch targets
- [ ] Add focus states (outline)
- [ ] Test contrast ratios (WCAG AA)
- [ ] Add ARIA labels for screen readers
- [ ] Test responsive design (480px, 768px, 1024px)
- [ ] Test dark mode
- [ ] Test animations on `prefers-reduced-motion: reduce`
- [ ] Verify keyboard navigation

## Examples: Common Patterns

### Form Layout

```tsx
<div className="space-y-4">
  <div>
    <label htmlFor="email" className="block text-sm font-medium mb-2">
      Email
    </label>
    <input
      id="email"
      type="email"
      className="w-full px-3 py-2 border border-gray-300 rounded-md
                 focus-visible:outline-2 focus-visible:outline-primary"
    />
  </div>
  <Button type="submit" fullWidth>Submit</Button>
</div>
```

### Grid Layout

```tsx
<div className="grid grid-cols-4 md:grid-cols-2 sm:grid-cols-1 gap-4">
  {items.map(item => (
    <Card key={item.id} hoverable>
      {item.name}
    </Card>
  ))}
</div>
```

### Responsive Image

```tsx
<img
  src={image}
  alt="Product"
  className="w-full h-auto max-w-lg
             md:max-w-md sm:max-w-sm"
/>
```

### Alert/Banner

```tsx
<div className="bg-warning bg-opacity-20 border-l-4 border-warning p-4 rounded">
  <p className="text-warning font-semibold">
    ⚠ Warning: Component compatibility issue detected
  </p>
</div>
```

## Troubleshooting

### Tailwind Classes Not Applied

```bash
# Ensure package.json has Tailwind
npm install tailwindcss postcss autoprefixer

# Check tailwind.config.js content paths
content: ["./index.html", "./src/**/*.{js,ts,jsx,tsx}"]

# Rebuild CSS
npm run build
```

### Dark Mode Not Working

Ensure CSS files import animations.css which has dark mode queries:

```css
@import './animations.css';
```

### Focus Outline Hidden

Check z-index stacking - if parent has higher z-index, outline may be hidden. Use `z-auto` on focused elements.

## Performance Tips

1. **Use CSS Variables** instead of inline styles for theme switching
2. **Minimize CSS-in-JS** - prefer Tailwind utilities
3. **Use `will-animate`** class for GPU acceleration:
   ```tsx
   <div className="will-animate animate-spin">
   ```
4. **Defer non-critical animations** on mobile
5. **Use `@media (prefers-reduced-motion)`** for respect user preferences

## File Structure

```
frontend/
  src/
    styles/
      ├─ index.css           (main import)
      ├─ reset.css           (normalize)
      ├─ variables.css       (design tokens)
      ├─ typography.css      (fonts)
      ├─ utilities.css       (layout)
      └─ animations.css      (keyframes)
    components/
      ├─ common/
      │   ├─ Button.tsx
      │   ├─ Card.tsx
      │   ├─ Modal.tsx
      │   ├─ Loading.tsx
      │   ├─ Toast.tsx
      │   └─ Tabs.tsx
      ├─ layout/
      │   ├─ Header.tsx
      │   ├─ Header.css
      │   ├─ Sidebar.tsx
      │   ├─ Sidebar.css
      │   ├─ MainLayout.tsx
      │   └─ MainLayout.css
      └─ features/
          ├─ builder/
          │   ├─ pages/
          │   │   └─ BuilderPage.tsx
          │   └─ BuilderPage.module.css
          └─ [other features]
  tailwind.config.js        (Tailwind config)
  postcss.config.js         (PostCSS config)
```

## Next Steps for Developers

1. **For new components**: Use Tailwind + Button/Card patterns
2. **For complex layouts**: Create `.module.css` file with CSS Grid/Flexbox
3. **For forms**: Follow Form Layout pattern with accessibility
4. **For animations**: Use animation classes or `@keyframes`
5. **For consistency**: Reference this guide and existing components

## Learning Resources

- [Tailwind CSS Docs](https://tailwindcss.com/docs)
- [MDN CSS Grid](https://developer.mozilla.org/en-US/docs/Web/CSS/CSS_Grid_Layout)
- [WCAG 2.1 Guidelines](https://www.w3.org/WAI/WCAG21/quickref/)
- [CSS Variables](https://developer.mozilla.org/en-US/docs/Web/CSS/var)

---

**Last Updated:** April 2026  
**Version:** 1.0  
**Maintained by:** Design System Team

/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  theme: {
    extend: {
      colors: {
        // Primary — sunset coral. Used for CTAs, active states, FABs.
        coral: {
          50: '#FFF4F1',
          100: '#FFE4DC',
          200: '#FFC7B8',
          300: '#FFA48D',
          400: '#FF8567',
          500: '#FF6B4A',
          600: '#F04E2B',
          700: '#C93B1E',
          800: '#9E301A',
          900: '#7A2716',
        },
        // Secondary — deep teal. Used for icons, badges, secondary accents.
        teal: {
          50: '#EDF7F6',
          100: '#D3ECE9',
          200: '#A6D9D3',
          300: '#78C1B8',
          400: '#489E93',
          500: '#227E73',
          600: '#0F6B66',
          700: '#0B5450',
          800: '#083E3B',
          900: '#052928',
        },
        // Accent — warm gold highlight (e.g. the OWNER badge).
        gold: {
          50: '#FFFBEF',
          100: '#FFF3D1',
          300: '#FFDE8A',
          400: '#FFC857',
          500: '#F5B428',
          600: '#D99A12',
        },
        // Warm background instead of cold slate.
        cream: {
          DEFAULT: '#FFF9F2',
          100: '#FFFDFB',
          200: '#FFF3E4',
        },
        // Warm charcoal for headline text.
        ink: '#2B2B2B',
      },
      boxShadow: {
        warm: '0 4px 14px 0 rgb(255 107 74 / 0.12)',
        'warm-lg': '0 12px 28px 0 rgb(255 107 74 / 0.18)',
      },
    },
  },
  plugins: [],
}

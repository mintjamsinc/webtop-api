import {terser} from 'rollup-plugin-terser';

export default {
  input: 'src/auth.js',
  output: {
    file: 'dist/auth.min.js',
    format: 'es',
    plugins: [terser()]
  }
};

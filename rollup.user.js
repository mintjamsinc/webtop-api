import {terser} from 'rollup-plugin-terser';

export default {
  input: 'src/user.js',
  output: {
    file: 'dist/user.min.js',
    format: 'es',
    plugins: [terser()]
  }
};

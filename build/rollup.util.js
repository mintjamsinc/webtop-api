import {terser} from 'rollup-plugin-terser';

export default {
  input: 'src/util.js',
  output: {
    file: 'dist/util.min.js',
    format: 'es',
    plugins: [terser()]
  }
};

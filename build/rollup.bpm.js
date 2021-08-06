import {terser} from 'rollup-plugin-terser';

export default {
  input: 'src/bpm.js',
  output: {
    file: 'dist/bpm.min.js',
    format: 'es',
    plugins: [terser()]
  }
};

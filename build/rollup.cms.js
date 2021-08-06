import {terser} from 'rollup-plugin-terser';

export default {
  input: 'src/cms.js',
  output: {
    file: 'dist/cms.min.js',
    format: 'es',
    plugins: [terser()]
  }
};

# TODO
(In no particular order)

- Implement boolean and/or (`&&` / `||`) operators
- Implement string operators
  - `-` => `'asdf' - 2 = 'as'` and `'asdf' - 5 = ''` and `'asdf' - -1 = 'asdf'`
  - `/` => `'asdf' / 2 = ['as', 'df']` and `'asdf' / 3 = ['a', 's', 'df']` and `'asdf' / 5 = ['a', 's', 'd', 'f', nil]`
- Type inference (pass "guess" of type into the typechecking methods)
   - Overloaded functions (functions named the same and return the same, but with different params)
   ✓ Support lazy evaluation of types (by storing AST along with binding name in TypeEnvironment?)
     - e.g. `let identity = a => a; a(5)`
✓ Field-wise type declaration (see Person type in test.meg)
- Parametrized types (e.g. `SomeType[T, U, V]`)
- Property accessors of existing types (e.g. `let s = 'asdf'; s.length`)
- Property accessors of type instances (e.g. `let a: Person = { name: 'Ken' }; a.name`)
- Union type declaration (see `PrimitiveTypes.NUMBER`, which is a precursor to this)
- Add line/col numbers to Tokens / Error messages
- Single-line comments
- Multi-line comments
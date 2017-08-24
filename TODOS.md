# TODO
(In no particular order)

- Implement boolean and/or (`&&` / `||`) operators
- Type inference (pass "guess" of type into the typechecking methods)
✓ Field-wise type declaration (see Person type in test.meg)
- Parametrized types (e.g. `SomeType[T, U, V]`)
- Property accessors of existing types (e.g. `let s = 'asdf'; s.length`)
- Property accessors of type instances (e.g. `let a: Person = { name: 'Ken' }; a.name`)
- Union type declaration (see `PrimitiveTypes.NUMBER`, which is a precursor to this)
- Add line/col numbers to Tokens / Error messages
- Single-line comments
- Multi-line comments
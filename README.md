# mega-jvm
A toy programming language written on the JVM

[![Build Status](https://travis-ci.org/kengorab/mega-jvm.svg?branch=master)](https://travis-ci.org/kengorab/mega-jvm)

This project differs from [other programming languages I've tried my hand at](https://github.com/kengorab/kage) because it doesn't rely on a parser generator; instead I employed the technique of [Pratt Parsing](https://en.wikipedia.org/wiki/Pratt_parser). I think this allows greater control over the language (as well as error messages), but more importantly provides better insight into how the language works (which is the whole reason I undertake projects like this anyway).

## What's it look like?

#### `examples/module1.meg`
```kotlin
import Person, greet from 'examples/person'

val person = Person(name: 'Ken', phoneNumber: 5558675309)
val greeting = greet(person)
```

#### `examples/person.meg`
```kotlin
export type Person = {
  name: String,
  phoneNumber: Int
}

export func greet(p: Person, greeting: String = 'Hello') = greeting + ' ' + p.name
```

## Todos
I typically like to keep track of todo items using a `TODOs.md` file in my projects. However, recently I've been using Trello for this; this project's Trello board is [here](https://trello.com/b/3kYZblAJ/mega-jvm).

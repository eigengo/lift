func identity<A>(a: A) -> A {
    return a
}

func const<A, B>(c: A) -> B -> A {
    return { _ in return c }
}

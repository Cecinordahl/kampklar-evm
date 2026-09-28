/** Shared loading / error / empty states, so every page fails the same readable way. */
export function Loading() {
  return <p className="muted" role="status">Laster…</p>;
}

export function LoadError({ error }: { error: Error }) {
  return (
    <p className="error" role="alert">
      Klarte ikke å hente data. Prøv å laste siden på nytt.
      <span className="visually-hidden"> ({error.message})</span>
    </p>
  );
}

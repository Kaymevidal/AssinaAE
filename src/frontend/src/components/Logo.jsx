/**
 * Logo do AssinaAE: "A" inicial e o "AE" final em azul (cor da marca),
 * "ssina" em branco com contorno preto fino — como pedido pelo dono do produto.
 */
export default function Logo({ className }) {
  return (
    <svg
      viewBox="0 0 300 64"
      className={className}
      role="img"
      aria-label="AssinaAE"
      xmlns="http://www.w3.org/2000/svg"
    >
      <text
        x="4"
        y="47"
        fontFamily="system-ui, -apple-system, Segoe UI, Roboto, sans-serif"
        fontWeight="800"
        fontSize="44"
      >
        <tspan fill="#2563eb">A</tspan>
        <tspan fill="#ffffff" stroke="#111827" strokeWidth="1.4">ssina</tspan>
        <tspan fill="#2563eb">AE</tspan>
      </text>
    </svg>
  );
}

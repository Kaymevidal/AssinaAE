import { useEffect, useRef } from 'react';

const CLIENT_ID = import.meta.env.VITE_GOOGLE_CLIENT_ID;

export default function BotaoGoogle({ onCredential }) {
  const containerRef = useRef(null);

  useEffect(() => {
    if (!CLIENT_ID || !window.google?.accounts?.id) return;

    window.google.accounts.id.initialize({
      client_id: CLIENT_ID,
      callback: (resposta) => onCredential(resposta.credential),
    });

    window.google.accounts.id.renderButton(containerRef.current, {
      type: 'standard',
      theme: 'outline',
      size: 'large',
      width: 320,
    });
  }, [onCredential]);

  if (!CLIENT_ID) return null;

  return <div ref={containerRef} />;
}

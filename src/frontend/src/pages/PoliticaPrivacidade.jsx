export default function PoliticaPrivacidade() {
  return (
    <div className="card documento-legal">
      <h2>Política de Privacidade</h2>
      <p className="documento-legal__atualizacao">Última atualização: setembro de 2026</p>

      <p>
        Esta política explica quais dados o AssinaAE coleta, para quê e com quem são compartilhados,
        em conformidade com a Lei Geral de Proteção de Dados (LGPD — Lei nº 13.709/2018).
      </p>

      <h3>1. Quem é o controlador</h3>
      <p>
        O AssinaAE é operado por Kayo Meira Vidal. Dúvidas ou pedidos relacionados a dados pessoais podem
        ser enviados para <a href="mailto:kayomeiravidal@gmail.com">kayomeiravidal@gmail.com</a>.
      </p>

      <h3>2. Quais dados coletamos</h3>
      <ul>
        <li><strong>Do profissional (quem cria conta):</strong> nome, email e, se o cadastro for por senha, o hash da senha (nunca a senha em texto puro). Se o login for feito com o Google, recebemos o identificador da conta Google, nome e email associados.</li>
        <li><strong>Do cliente (quem recebe o link de assinatura):</strong> nome e email informados pelo profissional ao criar o contrato. O cliente não cria conta.</li>
        <li><strong>Conteúdo dos contratos:</strong> o PDF enviado pelo profissional e a imagem da assinatura desenhada por cada parte na tela, que é embutida no PDF final.</li>
        <li><strong>Dados técnicos:</strong> endereço IP, usado apenas para limitar tentativas abusivas de login/cadastro (proteção contra força bruta).</li>
      </ul>

      <h3>3. Para que usamos esses dados</h3>
      <ul>
        <li>Autenticar o profissional e manter a sessão logada.</li>
        <li>Enviar os links de assinatura, confirmações de email e notificações de contrato assinado/recusado.</li>
        <li>Gerar e armazenar o PDF assinado por cada parte.</li>
        <li>Proteger a plataforma contra uso abusivo (limite de tentativas por IP).</li>
      </ul>

      <h3>4. Com quem compartilhamos</h3>
      <p>Não vendemos nem compartilhamos dados com terceiros para fins de publicidade. Usamos os seguintes prestadores de serviço, estritamente para operar a plataforma:</p>
      <ul>
        <li><strong>SendGrid</strong> — envio dos emails transacionais (convites de assinatura, verificação de conta, PDF final).</li>
        <li><strong>Google</strong> — validação do login via Google (quando essa opção é usada).</li>
        <li><strong>Neon e Railway</strong> — hospedagem do banco de dados e do backend, respectivamente.</li>
      </ul>

      <h3>5. Como os dados são armazenados</h3>
      <p>
        Senhas são armazenadas como hash (nunca em texto puro). A sessão do profissional usa um token (JWT)
        guardado no armazenamento local do navegador, que expira automaticamente. As conexões com o servidor
        são feitas via HTTPS.
      </p>

      <h3>6. Seus direitos</h3>
      <p>
        Conforme a LGPD, você pode solicitar a qualquer momento acesso, correção, exclusão ou portabilidade
        dos seus dados, além de retirar consentimentos dados anteriormente. Basta entrar em contato pelo
        email acima.
      </p>

      <h3>7. Retenção de dados</h3>
      <p>
        Os dados da conta e dos contratos são mantidos enquanto a conta estiver ativa ou enquanto forem
        necessários para as finalidades descritas nesta política, podendo ser excluídos a pedido do titular,
        salvo obrigação legal de retenção.
      </p>

      <h3>8. Alterações desta política</h3>
      <p>Esta política pode ser atualizada. Mudanças relevantes serão refletidas na data no topo desta página.</p>
    </div>
  );
}

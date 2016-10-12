# Relatório 1 - *Software Processes*

## Descrição do projeto

O projeto [**“ownCloud”**](https://owncloud.org/), desenvolvido de uma forma transparente pela comunidade *open source*, corresponde a uma [**aplicação**](https://play.google.com/store/apps/details?id=com.owncloud.android) para a plataforma Android que guarda ficheiros na nuvem.

![owncloud](/ESOF-docs/resources/ownCloud2.png)

A aplicação permite:
* Guardar ficheiros, calendários, contatos e emails;
* Partilhar os ficheiros com outros utilizadores ou grupos de utilizadores;
* Definir datas de expiração da partilha;
* Proteger a partilha com palavra-chave;
* Permitir ou não editar.

Posto isto, tanto utilizadores particulares que usem um [**servidor gratuito**](https://owncloud.org/providers/), assim como grandes empresas que utilizem uma [**subscrição empresarial**](https://owncloud.com/), podem ter os seus ficheiros sincronizados de uma forma segura e descomplicada em todos os seus dispositivos, uma vez que são estes que controlam os servidores.

A aplicação permite usar várias contas e trocar entre elas, mas só uma conta pode estar a uso de cada vez. Fornece algumas vantagens relativamente à interface Web, uma vez que permite uma sincronização automática dos ficheiros, assim como adicionar ficheiros diretamente do armazenamento do dispositivo.

Duas aplicações familiares a todos e que são bastante parecidas com esta são o “Google Drive” e o “OneDrive”.

## Processos de desenvolvimento

### Descrição do processo de desenvolvimento

O processo de desenvolvimento adotado pelos programadores foi o "Test-Driven Development". 
O projeto tem uma lista de objetivos planeados e cada novo contribuidor cria a lista de testes a serem cumpridos. Depois o contribuidor desenvolve o código de maneira a passar nesses novos testes. Caso não passe em todos os nos novos testes e não seja fundamental ao bom funcionamento do programa, pode adicionar uma nova issue a pedir contribuições à comunidade para resolover o problema. Se passar pode também ser gerado uma nova issue com o intuito de melhorar o código, ou com a ideia de uma nova funcionalidade complementar.

#### Atividade

O projeto encontra-se ativo com uma média de 20 commits por semana, no último ano.
No momento em que redigimos o relatório, existiam 306 *issues*. Em relação aos *pull requests*, existiam 46 pedidos em aberto, o que demonstra que este projeto se encontra bastante ativo, com cerca de 50 contribuidores.

#### Objetivos

Existem 2 versões planeadas para serem lançadas. A primeira versão encontra-se 23% completa, sendo atualizada com bastante regularidade. No que diz respeito à segunda versão, ainda não se encontra a ser implementada.

### Opiniões, Críticas e Alternativas

Na opinião do grupo, o processo de desenvolvimento usado é uma boa opção, pois torna-se fácil de criar novos objetivos/funcionalidades com base na complexidade necessária para criar os testes. As funções em desenvolvimento não precisam de funcionar perfeitamente, bastanto que passem nos testes. Deste modo, é possível avançar nos objetivos, colocando estas funções como *issue*, de forma a que sejam melhoradas posteriormente. No entanto, este método cria *issues* em demasia, que com um pouco mais de trabalho seriam desnecessárias. Ao adotar este processo de desenvolvimento, os desenvolvedores deixam muitos *issues* em aberto que não são resolvidos.

Outros processos de desenvolvimento também aplicáveis seriam *Software prototyping* e *Incremental development and delivery*.


Ver: android/user_manual/android_app.rst (diretório no repositório do GitHub)

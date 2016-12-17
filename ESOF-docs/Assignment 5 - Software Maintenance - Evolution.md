# Relatório 5: Manutenção/Evolução do *Software*

O objetivo deste relatório é documentar o processo seguido para desenvolver a funcionalidade que adicionamos ao projeto e abordar a análise do impacto das alterações realizadas.

## Manutenção do *software* (métricas SIG)

[![BCH compliance](https://bettercodehub.com/edge/badge/PauloSantos13/android)](https://bettercodehub.com)

Utilizamos a ferramenta [**Better Code Hub**](https://bettercodehub.com) para avaliar a qualidade do *Own Cloud*. A avaliação é feita com os seguintes tópicos:

    • Escrever pequenos pedaços de código
    • Escrever pedaços de código simples
    • Repetir código
    • Manter as unidades de interface pequenas
    • Separar funcionalidades em módulos
    • Arquitetura com componentes independentes
    • Manter as componentes arquiteturais equilibradas
    • Manter a base de código pequena
    • Automação de testes
    • Desenvolvimento de código estruturado e otimizado

Analisando estas características, o nosso projeto obteve uma classificação de 3 em 10.

Discuss Software Maintainability using the SIG metrics (plus add the badge to this .md file).

## Processo de evolução (análise do impacto das alterações e implementação)

Após uma análise detalhada das funcionalidades que a aplicaçao do **ownCloud** permite ao utilizador, o grupo achou que estava em falta a informação sobre o espaço que o utilizador já ocupou na *cloud* relativamente ao espaço total, dado o propósito da aplicação.

Desta forma, falamos com o responsável pela aplicação, e este deu-nos algumas dicas por onde devíamos começar, concordando que era uma *feature* interessante para ser implementada.

Recorrendo à funcionalidade *Android Monitor* do *Android Studio*, o grupo tentou entender que módulos eram chamados enquanto a aplicação corria, descobrindo que partes de código seriam necessárias alterar.

Uma vez que ao iniciar a aplicação é logo chamada a operação **RefreshFolderOperation** do *package* *com.owncloud.android.operations*, pensamos ser o melhor sítio para proceder à chamada de uma nova função **updateUserQuota**, atualizando as informações acerca do espaço ocupado sempre que o utilizador faz *refresh* da página.

![callUpdateUserQuota](/ESOF-docs/resources/callUpdateUserQuota.PNG)

A função **updateUserQuota** consiste na criação e execução de uma instância da classe **GetRemoteUserQuotaOperation** (contida no *package com.owncloud.android.lib.resources.users*). Esta execução permite aceder às informações da quota de espaço de um utilizador, no servidor onde este possui a sua conta, através dos métodos *getFree()*, *getUsed()* e *getTotal()*. 

![updateUserQuota](/ESOF-docs/resources/updateUserQuota.PNG)

Como estes dados têm que ser guardados numa forma persistente para conseguirmos aceder a estes noutros módulos, recorremos à base de dados local (**mStoreManager**, instância da classe **FileDataStorageManager** do *package com.owncloud.android.datamodel*) onde criamos uma função **setQuota** para definirmos os dados.

![fileDataStoreManager](/ESOF-docs/resources/fileDataStoreManager.PNG)

Achamos que o melhor local para mostrar ao utilizador esta informação seria no *footer* da página inicial, uma vez que neste já era mostrado o número de ficheiros e pastas que existiam na *cloud*, completando assim essa informação. Verificamos que este era atualizado na função **updateLayout** da classe **OCFileListFragment** do *package com.owncloud.android.ui.fragment*. Esta função é chamada por outra denominada **listDirectory**, e é nesta que vamos buscar os dados que armazenamos anteriormente na base de dados acerca da quota do utilizador.

![listDirectory](/ESOF-docs/resources/listDirectory.PNG)

A função **updateLayout** recebe agora os parâmetros *usedQuota* e *totalQuota*, definindo o *footer* com os novos dados.

![updateLayout](/ESOF-docs/resources/updateLayout.PNG)

Esse *footer* é criado na função **generateFooterText**, onde inicialmente era mostrado apenas o número de ficheiros e pastas existentes no diretório raiz, contendo agora as informações sobre o espaço utilizado e o espaço total do servidor no qual o utilizador possui uma conta.

![generateFooterText](/ESOF-docs/resources/generateFooterText.PNG)

Faz-se uso da função **formatShortFileSize** para formatar o tamanho do espaço utilizado e total para o mais adequado, uma vez que os parâmetros *usedQuota* e *totalQuota* são expressos em *bytes*.

Mostra-se, a seguir, o resultado antes e depois das alterações: 

<img src="/ESOF-docs/resources/beforeChanges.png" align="center" width="400" >
<img src="/ESOF-docs/resources/afterChanges.png" align="center" width="400" >

## Link para o *pull request*

## Contribuições

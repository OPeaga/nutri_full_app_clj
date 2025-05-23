O que a aplicação app_nutri deve ter:
1) Front
2) Backend

Atribuições de cada stack:
1- Front (Cliente)
* Capaz de receber valores do usuário
* Criar menus interativos a partir das escolhas do usuário
* Comunicar com um controller por meio de endpoints
* Ser capaz de mostrar informações do usuário, como informações gerais e específicas com base nas entradas armazenadas no ATOM


2- Back-end (Servidor)
* Gerar requisições de método GET para apis de terceiros para fundamentar operações no front (como consulta de calorias / cálculos de calorias gastas)
* Gravar através de enpoints POST no ATOM
* Consultar por meio de endpoints GET no ATOM

3- Handler (Intermediário)
* Receber a solicitação do Front e garantir que o servidor em outra máquina compreenda a solicitação
* Monta a estrutura necessária para que o BACK possa ler
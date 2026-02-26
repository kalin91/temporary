Feature: Order Data Fetcher

  Background:
    * url baseUrl
    * path basePath

  Scenario: Get Order by ID
    Given header Authorization = authHeader('admin')
    And def query = read('GetOrder.graphql')
    And def variables = { id: '42' }
    And request { query: '#(query)', variables: '#(variables)' }
    When method post
    Then status 200
    And match response.data.order ==
    """
    {
      id: "42",
      orderDate: '#regex ^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}Z$',
      status: '#string',
      totalAmount: '#number',
      customer: {
        id: '#string',
        firstName: '#string',
        lastName: '#string',
        email: '#string'
      }
    }
    """

  Scenario: Create Order
    Given header Authorization = authHeader('admin')
    And def query = read('CreateOrder.graphql')
    And def variables =
    """
    {
      "input": {
        "customerId": "1",
        "totalAmount": 299.99
      }
    }
    """
    And request { query: '#(query)', variables: '#(variables)' }
    When method post
    Then status 200
    And match response.data.createOrder.id == '#string'
    And match response.data.createOrder.totalAmount == variables.input.totalAmount
    And match response.data.createOrder.status == 'PENDING'
    And match response.data.createOrder.customer.id == variables.input.customerId
    And match response.data.createOrder.orderDate == '#notnull'

  Scenario: Get Orders by Customer and Status
    Given header Authorization = authHeader('admin')
    And def query = read('GetOrdersByCustomerAndStatus.graphql')
    And def variables =
    """
    {
      "customerId": "1",
      "status": "DELIVERED",
      "page": 0,
      "size": 10
    }
    """
    And request { query: '#(query)', variables: '#(variables)' }
    When method post
    Then status 200
    And match response.data.orders.content == '#[]'
    
    # Validate that every returned order matches the requested status and customer ID
    And match each response.data.orders.content[*].status == variables.status
    And match each response.data.orders.content[*].customer.id == variables.customerId

  Scenario: Update Order
    Given header Authorization = authHeader('admin')
    And def query = read('UpdateOrder.graphql')
    And def variables =
    """
    {
      "id": "42",
      "input": {
        "status": "SHIPPED",
        "totalAmount": 11929.99
      }
    }
    """
    And request { query: '#(query)', variables: '#(variables)' }
    When method post
    Then status 200
    And match response.data.updateOrder.id == variables.id
    And match response.data.updateOrder.status == variables.input.status
    And match response.data.updateOrder.totalAmount == variables.input.totalAmount

  Scenario: Delete Order
    Given header Authorization = authHeader('admin')
    And def query = read('DeleteOrder.graphql')
    And def variables = { "id": "39" }
    And request { query: '#(query)', variables: '#(variables)' }
    When method post
    Then status 200
    And match response.data.deleteOrder == true

  Scenario: Reader cannot create an order
    # ROLE_READER only has read access – createOrder requires ROLE_WRITER or higher.
    # The API returns HTTP 200 with a GraphQL errors array (no data).
    Given header Authorization = authHeader('reader')
    And def query = read('CreateOrder.graphql')
    And def variables =
    """
    {
      "input": {
        "customerId": "1",
        "totalAmount": 9.99
      }
    }
    """
    And request { query: '#(query)', variables: '#(variables)' }
    When method post
    Then status 200
    And match response.errors != null
    And match response.errors[0].message == 'Forbidden'
    And match response.data.createOrder == '#notpresent'

  Scenario: Writer cannot delete an order
    # ROLE_WRITER can create/update but not delete – deleteOrder requires ROLE_ADMIN.
    # The API returns HTTP 200 with a GraphQL errors array (no data).
    Given header Authorization = authHeader('writer')
    And def query = read('DeleteOrder.graphql')
    And def variables = { "id": "1" }
    And request { query: '#(query)', variables: '#(variables)' }
    When method post
    Then status 200
    And match response.errors != null
    And match response.errors[0].message == 'Forbidden'
    And match response.data.deleteOrder == '#notpresent'

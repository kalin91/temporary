Feature: Customer Data Fetcher Tests

  Background:
    * url baseUrl
    * path basePath

  Scenario: Get Customer by ID with Orders
    Given header Authorization = authHeader('admin')
    And def query = read('GetCustomer.graphql')
    And def variables =
    """
    {
      "id": "4",
      "orderStatus": null,
      "orderPage": 0,
      "orderSize": 10
    }
    """
    And request { query: '#(query)', variables: '#(variables)' }
    When method post
    Then status 200
    And match response.data.customer.id == '#string'
    And match response.data.customer.firstName == '#string'
    And match response.data.customer.lastName == '#string'
    And match response.data.customer.email == '#string'
    And match response.data.customer.orders == '#[]'
    And match each response.data.customer.orders ==
    """
    {
      id: '#string',
      customer: {
        id: '#string'
      },
      orderDate: '#regex ^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}Z$',
      status: '#string',
      totalAmount: '#number'
    }
    """

  Scenario: Get Customers with Pagination
    Given header Authorization = authHeader('admin')
    And def query = read('GetCustomers.graphql')
    And def variables =
    """
    {
      "page": 0,
      "size": 100,
      "orderStatus": null,
      "orderPage": 0,
      "orderSize": 100
    }
    """
    And request { query: '#(query)', variables: '#(variables)' }
    When method post
    Then status 200
    And match response.data.customers.totalPages == '#number'
    And match response.data.customers.totalElements == '#number'
    And match response.data.customers.number == '#number'
    And match response.data.customers.content == '#[]'
    
    # Check structure of each customer in the content list
    And match each response.data.customers.content ==
    """
    {
      id: '#string',
      firstName: '#string',
      lastName: '#string',
      email: '#string',
      orders: '#[]'
    }
    """
    
    # Check nested orders structure using JSON path wildcard
    # This validates that EVERY order within EVERY customer matches the schema
    And match each response.data.customers.content[*].orders[*] ==
    """
    {
      id: '#string',
      status: '#string',
      totalAmount: '#number'
    }
    """

  Scenario: Create Customer
    Given header Authorization = authHeader('admin')
    And def query = read('CreateCustomer.graphql')
    And def variables =
    """
    {
      "input": {
        "firstName": "Cdarlos",
        "lastName": "Lódpez",
        "email": "carlos.lopxez@example.com"
      }
    }
    """
    And request { query: '#(query)', variables: '#(variables)' }
    When method post
    Then status 200
    And match response.data.createCustomer.id == '#string'
    And match response.data.createCustomer.firstName == variables.input.firstName
    And match response.data.createCustomer.lastName == variables.input.lastName
    And match response.data.createCustomer.email == variables.input.email

  Scenario: Update Customer
    Given header Authorization = authHeader('admin')
    And def query = read('UpdateCustomer.graphql')
    And def variables =
    """
    {
      "id": "17",
      "input": {
        "firstName": "Ariadna",
        "lastName": "Gonzalez",
        "email": "ariadna.gonzalez@example.com"
      }
    }
    """
    And request { query: '#(query)', variables: '#(variables)' }
    When method post
    Then status 200
    And match response.data.updateCustomer.id == variables.id
    And match response.data.updateCustomer.firstName == variables.input.firstName
    And match response.data.updateCustomer.lastName == variables.input.lastName
    And match response.data.updateCustomer.email == variables.input.email

  Scenario: Delete Customer
    Given header Authorization = authHeader('admin')
    And def query = read('DeleteCustomer.graphql')
    And def variables = { "id": "12" }
    And request { query: '#(query)', variables: '#(variables)' }
    When method post
    Then status 200
    And match response.data.deleteCustomer == true

  Scenario: Reader cannot create a customer
    # ROLE_READER only has read access – createCustomer requires ROLE_WRITER or higher.
    # The API returns HTTP 200 with a GraphQL errors array (no data).
    Given header Authorization = authHeader('reader')
    And def query = read('CreateCustomer.graphql')
    And def variables =
    """
    {
      "input": {
        "firstName": "Unauthorized",
        "lastName": "User",
        "email": "unauthorized@example.com"
      }
    }
    """
    And request { query: '#(query)', variables: '#(variables)' }
    When method post
    Then status 200
    And match response.errors != null
    And match response.data.createCustomer == '#notpresent'

  Scenario: Writer cannot delete a customer
    # ROLE_WRITER can create/update but not delete – deleteCustomer requires ROLE_ADMIN.
    # The API returns HTTP 200 with a GraphQL errors array (no data).
    Given header Authorization = authHeader('writer')
    And def query = read('DeleteCustomer.graphql')
    And def variables = { "id": "1" }
    And request { query: '#(query)', variables: '#(variables)' }
    When method post
    Then status 200
    And match response.errors != null
    And match response.data.deleteCustomer == '#notpresent'


![Logo image](img/sbtscalasamlogo_small.png)

# sam-dynamodb-seed.g8
A template project for quickly creating stateful serverless applications using dynamodb.

## Introduction
The seed shows how to:

- setup a `AWS::DynamoDB::Table` resosurce
- how to handle get and post requests
- how to save and load using dynamodb tables

## Usage
Create a new template project by typing:

```
sbt new dnvriend/sam-dynamodb-seed.g8
```

## Deploying
To deploy, you need to have your AWS account setup correctly:

- To deploy type: `samDeploy`,
- To remove type: `samRemove`,
- To get more info type: `samInfo`,
- To view the CloudFormation template type: `samValidate`.

For more information, please see the [dnvriend/sbt-sam](https://github.com/dnvriend/sbt-sam) repository.

## Using the example
Posting a person:

```
$ http post https://q2irbnzbj5.execute-api.eu-west-1.amazonaws.com/dnvriend/person name=dennis age:=42 lucky_number:=10
HTTP/1.1 200 OK
{
    "age": 42,
    "id": "410a5de5-cfbf-45c6-9fb5-4571c3899568",
    "lucky_number": 10,
    "name": "dennis"
}
```

Getting a person:

```
$ http https://q2irbnzbj5.execute-api.eu-west-1.amazonaws.com/dnvriend/person/1
HTTP/1.1 404 Not Found
{
    "msg": "person with id '1' not found"
}
```

```
$ http post https://q2irbnzbj5.execute-api.eu-west-1.amazonaws.com/dnvriend/person name=dennis age:=42 lucky_number:=10
HTTP/1.1 200 OK
{
    "age": 42,
    "id": "1977c477-3dc0-4af0-b072-30b7454fc433",
    "lucky_number": 10,
    "name": "dennis"
}
```

## `AWS::DynamoDB::Table` resource configuration
The seed `sam-component` uses Typesafe configuration to setup resources. This seed focuses on the combination
of `AWS::Serverless::Function` that uses an `AWS::DynamoDB::Table` resource for its persistence requirements.

In the base directory, a folder `conf` is created, containing a file `sam.conf` that defines AWS resources.
`sbt-sam` plugin interprets the `sam.conf` and indexes the resources, in effect it 'knows' about AWS resources can 
in future releases will make use of this knowledge, over 'just' creating the CloudFormation template.

An example `sam.conf` that creates is:

```
dynamodb {
   // a simple table that has only a hash key 'id'
   People {
    name = people
    hash-key = {
      name = id
      type = S
    }
    rcu = 1
    wcu = 1
  }
  
   // a simple table that has only a hash key 'id', and 
   // has DynamoDB streams enabled with KEYS_ONLY
   People {
    name = people
    hash-key = {
      name = id
      type = S
    }
    stream = KEYS_ONLY
    rcu = 1
    wcu = 1
  }
  
    // a table that has has two keys, a hash 'name' and range key 'age' 
     People {
      name = people
      hash-key = {
        name = id
        type = S
      }
      range-key = {
        name = name
        type = N
      }
      rcu = 1
      wcu = 1
    }
    
  // a table that has has two keys, a hash 'name' and range key 'age' and
  // a global secondary index 
  People {
    name = people
    hash-key = {
      name = name
      type = S
    }
    range-key = {
      name = name
      type = N
    }

    global-secondary-indexes {
      people_id {
        hash-key = {
          name = id
          type = S
        }

        projection-type = ALL
        rcu = 1
        wcu = 1
      }
    }
    rcu = 1
    wcu = 1
  }
``` 

## Defining multiple tables
To define multiple tables, just put them in the same `dynamodb` block, and give them a new name:

```
dynamodb {
   People {
    name = people
    hash-key = {
      name = id
      type = S
    }
    rcu = 1
    wcu = 1
  }
  Addresses {
      name = addresses
      hash-key = {
        name = id
        type = S
      }
      rcu = 1
      wcu = 1
  }
  Orders {
    name = orders
     hash-key = {
        name = id
        type = S
     }
     rcu = 1
     wcu = 1
  }
```

## Table names
Because resources in a single AWS account has a flat namespace, a design decision has been made, in order to easily develop and
deploy resources without having resource name clash. This means that `sbt-sam` knows about the name of a resource in the project,
like eg. the `person` table, but will deploy the resource with a managed logical name with the following pattern:

```
[project-name][stage][resource-name]
```

For the table `person`, this means that the resource name will become:

```
sam-seed-dnvriend-people
```
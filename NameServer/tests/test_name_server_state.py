import unittest

from name_server.server_state import NameServerState
from name_server.server_exceptions import *
from name_server.service_entry import ServiceEntry

class TestServiceEntry(unittest.TestCase):
    def test_is_valid_addr_valid(self):
        try:
            ServiceEntry.is_valid_addr("localhost:8000")
        except InvalidServiceEntry:
            self.fail(f'Unexpected exception {repr(e)}')

    def test_is_valid_addr_invalid(self):
        try:
            ServiceEntry.is_valid_addr("localhost:string")
            self.fail(f"Expected localhost:string to fail in ServiceEntry instanciation")
        except InvalidServiceEntry:
            pass

    def test_init(self):
        try:
            ServiceEntry("A", "localhost:8000")
        except InvalidServiceEntry as e:
            self.fail(f"Unexpected exception {repr(e)}")

    def test_invalid_addr(self):
        try:
            ServiceEntry("A", "localhost:string")
            self.fail(f"Expected localhost:string to fail in ServiceEntry instanciation")
        except InvalidServiceEntry as e:
            pass


class TestSimpleNameServer(unittest.TestCase):
    def setUp(self):
        self.ns = NameServerState()

        self.ns._name_lookup_table['TupleSpace'] = [ServiceEntry('A', 'localhost:8000')]
    
    #
    # Private methods tests
    #

    def test_exsists_name_true(self):
        self.assertEqual(self.ns._exists_name('TupleSpace'), True)

    def test_exsists_name_false(self):
        self.assertEqual(self.ns._exists_name('InvalidName'), False)

    def test_exsists_entry_true(self):
        self.assertEqual(self.ns._exists_entry('TupleSpace', (ServiceEntry('A', 'localhost:8000'))), True)
    
    def test_exsists_entry_false_name(self):
        self.assertEqual(self.ns._exists_entry('InvalidName', (ServiceEntry('A', 'localhost:8000'))), False)

    def test_exsists_entry_false_qual(self):
        self.assertEqual(self.ns._exists_entry('TupleSpace', (ServiceEntry('B', 'localhost:8000'))), False)

    def test_exsists_entry_false_addr(self):
        self.assertEqual(self.ns._exists_entry('TupleSpace', (ServiceEntry('A', 'localhost:8001'))), False)

    #
    # Lookup Tests
    #

    def test_lookup_true_not_empty(self):
        self.assertNotEqual(self.ns.lookup('TupleSpace', 'A'), [])

    def test_lookup_true_entry(self):
        self.assertEqual(self.ns.lookup('TupleSpace', 'A'), [ServiceEntry(qual='A', addr='localhost:8000')])

    def test_lookup_false_entry(self):
        self.assertNotEqual(self.ns.lookup('TupleSpace', 'A'), [ServiceEntry(qual='A', addr='localhost:8001')])

    def test_lookup_false_name(self):
        self.assertEqual(self.ns.lookup('InvalidName', 'A'), [])

    def test_lookup_false_qual(self):
        self.assertEqual(self.ns.lookup('TupleSpace', 'B'), [])

    def test_lookup_false_both(self):
        self.assertEqual(self.ns.lookup('InvalidName', 'B'), [])
    
    def test_lookup_true_name_only(self):
        self.assertEqual(self.ns.lookup('TupleSpace', ''), [ServiceEntry(qual='A', addr='localhost:8000')])

    def test_lookup_false_name(self):
        self.assertEqual(self.ns.lookup('InvalidName', ''), [])

    def test_lookup_nothing(self):
        self.assertEqual(self.ns.lookup('', ''), [])
 
    def test_lookup_no_name(self):
        self.assertEqual(self.ns.lookup('', 'A'), [])

    #
    # Register tests
    #

    def test_register_n_lookup(self):
        try:
            self.ns.register('TupleSpace', 'B', 'localhost:8001')
        except (InvalidServiceEntry, InvalidRegisterRequest) as e:
            self.fail(f'Got unexpected exception {str(e)}')
        
        self.assertEqual(self.ns.lookup('TupleSpace', ''), [ServiceEntry(qual='A', addr='localhost:8000'), ServiceEntry(qual='B', addr='localhost:8001')])

    def test_register_n_lookup_2(self):
        try:
            self.ns.register('TupleSpace', 'B', 'localhost:8001')
            self.ns.register('TupleSpace', 'C', 'localhost:8002')
        except (InvalidServiceEntry, InvalidRegisterRequest) as e:
            self.fail(f'Got unexpected exception {str(e)}')
        
        self.assertEqual(self.ns.lookup('TupleSpace', ''), [ServiceEntry(qual='A', addr='localhost:8000'), ServiceEntry(qual='B', addr='localhost:8001'), ServiceEntry(qual='C', addr='localhost:8002')])

    def test_register_n_lookup_3(self):
        try:
            self.ns.register('TupleSpace', 'B', 'localhost:8001')
            self.ns.register('TupleSpace', 'C', 'localhost:8002')
        except (InvalidServiceEntry, InvalidRegisterRequest) as e:
            self.fail(f'Got unexpected exception {str(e)}')
        
        self.assertEqual(self.ns.lookup('TupleSpace', 'C'), [ServiceEntry(qual='C', addr='localhost:8002')])
        self.assertEqual(self.ns.lookup('TupleSpace', 'B'), [ServiceEntry(qual='B', addr='localhost:8001')])
        self.assertEqual(self.ns.lookup('TupleSpace', 'A'), [ServiceEntry(qual='A', addr='localhost:8000')])

    def test_register_invalid(self):
        try:
            self.ns.register('TupleSpace', 'C', 'localhost:string')
            self.fail(f"Expected failure didn't happen, InvalidServiceEntry exception wasn't raised for invalid addr")
        except InvalidServiceEntry:
            pass

        self.assertEqual(self.ns.lookup('TupleSpace', 'C'), [])
        self.assertEqual(self.ns.lookup('TupleSpace', ''), [ServiceEntry(qual='A', addr='localhost:8000')])

    def test_register_new_name(self):
        try:
            self.ns.register('NewName', 'A', 'localhost:9000')
        except Exception as e:
            self.fail(f"Unexpected exception {str(e)}")
        
        self.assertEqual(self.ns.lookup('NewName', ''), [ServiceEntry(qual='A', addr='localhost:9000')])
        self.assertEqual(self.ns.lookup('NewName', 'B'), [])
        self.assertEqual(self.ns.lookup('NewName', 'A'), [ServiceEntry(qual='A', addr='localhost:9000')])
    
    def test_register_duplicate(self):
        try:
            self.ns.register('TupleSpace', 'A', 'localhost:8000')
            self.fail(f'Expected exception on duplicate register')
        except InvalidRegisterRequest:
            pass
        except Exception as e:
            self.fail(f"Unexpected exception {str(e)}")
        
    #
    # Delete tests
    #

    def test_delete_true(self):
        try:
            self.ns.delete('TupleSpace', 'localhost:8000')
        except Exception as e:
            self.fail(f'Unexpected exception {str(e)}')

        self.assertEqual(self.ns.lookup('TupleSpace', ''), [])

    def test_delete_false(self):
        try:
            self.ns.delete('TupleSpace', 'localhost:8001')
            self.fail(f'Expected error on removing unexistent service {str(e)}')
        except InvalidDeleteRequest:
            pass
        except Exception as e:
            self.fail(f'Unexpected exception {str(e)}')

        self.assertEqual(self.ns.lookup('TupleSpace', ''), [ServiceEntry(qual='A', addr='localhost:8000')])
        self.assertEqual(self.ns.lookup('TupleSpace', 'A'), [ServiceEntry(qual='A', addr='localhost:8000')])

    def test_delete_all(self):
        try:
            self.ns.register('TupleSpace', 'B', 'localhost:8001')
        except Exception as e:
            self.fail(f'Unexpected exception {str(e)}')

        self.ns.delete('TupleSpace', 'localhost:8000')
        self.assertEqual(self.ns.lookup('TupleSpace', ''), [ServiceEntry(qual='B', addr='localhost:8001')])
        self.ns.delete('TupleSpace', 'localhost:8001')
        self.assertEqual(self.ns.lookup('TupleSpace', ''), [])

    def test_all(self):
        try:
            self.ns.register('TupleSpace', 'A', 'localhost:9000')
        except Exception as e:
            self.fail(f'Unexpected exception {str(e)}')
        
        self.assertEqual(self.ns.lookup('TupleSpace', 'A'), [ServiceEntry(qual='A', addr='localhost:8000'),  ServiceEntry(qual='A', addr='localhost:9000')])

        try:
            self.ns.delete('TupleSpace', 'localhost:8000')
        except Exception:
            self.fail(f'Unexpected exception {str(e)}')

        self.assertEqual(self.ns.lookup('TupleSpace', ''), [ServiceEntry(qual='A', addr='localhost:9000')])
        

class TestListsNameServer(unittest.TestCase):
    """ These tests should only be ran after the TestSimpleNameServer tests have been successfull """
    __records = [
        ('Tuple', 'A', 'localhost:8080'),
        ('Tuple', 'B', 'localhost:8081'),
        ('Tuple', 'C', 'localhost:8082'),
        ('Foo1', 'Z', 'localhost:8083'),
        ('Foo2', 'Z', 'localhost:8084'),
        ('Foo3', 'Z', 'localhost:8085'),
    ]

    def setUp(self):
        self.ns = NameServerState()

        for r in self.__records:
            try:
                self.ns.register(*r)
            except Exception:
                raise 
    
    def test_lookup_name_tuple(self):
        self.assertEqual(self.ns.lookup('Tuple', ''), [ServiceEntry(qual=r[1], addr=r[2]) for r in self.__records[0:3]])

    def test_lookup_name(self):
        self.assertEqual(self.ns.lookup('Foo1', ''), [ServiceEntry(qual='Z', addr='localhost:8083')])

    def test_lookup_name_invalid_qual(self):
        self.assertEqual(self.ns.lookup('Foo1', 'A'), [])
    
    def test_delete(self):
        try:
            self.ns.delete('Tuple', 'localhost:8082')
        except Exception as e:
            self.fail(f'Got unexpected exception {str(e)}')
        
        self.assertEqual(self.ns.lookup('Tuple', ''), [ServiceEntry(qual='A', addr='localhost:8080'), ServiceEntry(qual='B', addr='localhost:8081')])

    def test_delete_unexistant(self):
        try:
            self.ns.delete('Tuple', 'localhost:8083')
        except InvalidDeleteRequest:
            pass
        except Exception as e:
            self.fail(f'Got unexpected exception {str(e)}')
        
        self.assertEqual(self.ns.lookup('Tuple', ''), [ServiceEntry(qual='A', addr='localhost:8080'), ServiceEntry(qual='B', addr='localhost:8081'), ServiceEntry(qual='C', addr='localhost:8082')])

    def test_register_duplicate(self):
        try:
            self.ns.register('Tuple', 'A', 'localhost:8080')
        except InvalidRegisterRequest:
                pass
        except Exception as e:
            self.fail(f'Got unexpected exception {str(e)}')
        
        self.assertEqual(self.ns.lookup('Tuple', ''), [ServiceEntry(qual='A', addr='localhost:8080'), ServiceEntry(qual='B', addr='localhost:8081'), ServiceEntry(qual='C', addr='localhost:8082')])

if __name__ == '__main__':
    unittest.main(failfast=True)


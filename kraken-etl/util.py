# -*- coding: utf-8 -*-

# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

import logging
from datetime import datetime
import os
import re
import subprocess
import tempfile


logger = logging.getLogger('kraken-etl-util')
interval_hierarchies = {
    'hourly':  {
        'depth':                        4,
        'directory_format':             '%Y/%m/%d/%H',
        'hive_partition_format':        'year=%Y/month=%m/day=%d/hour=%H',
        'hive_partition_ddl_format':    'year=%Y,month=%m,day=%d,hour=%H',
    },
    'daily':   {
        'depth':                        3,
        'directory_format':             '%Y/%m/%d',
        'hive_partition_format':        'year=%Y/month=%m/day=%d',
        'hive_partition_ddl_format':    'year=%Y,month=%m,day=%d',
    },
    'monthly': {
        'depth':                        2,
        'directory_format':             '%Y/%m',
        'hive_partition_format':        'year=%Y/month=%m',
        'hive_partition_ddl_format':    'year=%Y,month=%m',
   },
    'yearly':  {
        'depth':                        1,
        'directory_format':             '%Y',
        'hive_partition_format':        'year=%Y',
        'hive_partition_ddl_format':    'year=%Y',
    },
}


def diff_datewise(left, right, left_parse=None, right_parse=None):
    """
    Parameters
        left        : a list of datetime strings or objects
        right       : a list of datetime strings or objects
        left_parse   : None if left contains datetimes, or strptime format
        right_parse  : None if right contains datetimes, or strptime format

    Returns
        A tuple of two sets:
        [0] : the datetime objects in left but not right
        [1] : the datetime objects in right but not left
    """
    
    if left_parse:
        left_set = set([
            datetime.strptime(l.strip(), left_parse)
            for l in left if len(l.strip())
        ])
    else:
        left_set = set(left)
    
    if right_parse:
        right_set = set([
            datetime.strptime(r.strip(), right_parse)
            for r in right if len(r.strip())
        ])
    else:
        right_set = set(right)
    
    return (left_set - right_set, right_set - left_set)


def timestamps_to_now(start, increment):
    """
    Generates timestamps from @start to datetime.now(), by @increment
    
    Parameters
        start       : the first generated timestamp
        increment   : the timedelta between the generated timestamps
    
    Returns
        A generator that goes from @start to datetime.now() - x,
        where x <= @increment
    """
    now = datetime.now()
    while start < now:
        yield start
        start += increment


def sh(command, check_return_code=True):
    """
    Execute a shell command and return the result.
    command can be an array or a string.  If it is
    a string, shell=True will be passed to subprocess.Popen.
    """

    if isinstance(command, list):
        shell = False
        command_string = ' '.join(command)
    else:
        shell = True
        command_string = command

    logger.debug('Running: {0}'.format(command_string))
    p = subprocess.Popen(command, stdout=subprocess.PIPE, stderr=subprocess.PIPE, shell=shell)
    stdout, stderr = p.communicate()
    if check_return_code and p.returncode != 0:
        raise RuntimeError("Command: {0} failed with error code: {1}".format(command_string, p.returncode),
                               stdout, stderr)
    return stdout.strip()


class HiveUtils(object):
    def __init__(self, database='default', options=''):
        self.database   = database
        if options:
            self.options    = options.split()
        else:
            self.options = []

        self.hivecmd = ['hive'] + self.options + ['cli', '--database', self.database]
        self.tables  = {}

    def add_missing_partitions(self, table):
        add_partition_ddl = self.get_missing_partitions_ddl(table)
        if (add_partition_ddl and len(add_partition_ddl)>0):
            self.query(add_partition_ddl)


    def get_missing_partitions_ddl(self, table):
        missing_partitions = self.get_missing_partitions(table) #['orange/2014/08/13', 'orange/2014/08/14']
        if len(missing_partitions) == 0:
            return

        partitions_definition = self.get_partitions_definition(table) #['tenant','year','month','day']

        #PARTITION (tenant='orange', year='2014', month='08', day='13') LOCATION 'orange/2014/08/13'
        #PARTITION (tenant='orange', year='2014', month='08', day='14') LOCATION 'orange/2014/08/14'

        part_ddls = []
        for mp in missing_partitions:
            #['ALTER TABLE %s ADD %s;' % (table, ddl) for table,ddl in zip(tables,ddls)]
            parts = ','.join(['%s=\'%s\'' % (part, val) for part, val in zip(partitions_definition, mp.split('/'))])
            part_ddl = ' '.join(['PARTITION (', parts, ') LOCATION', '\''+ mp + '\''])
            part_ddls.append(part_ddl)
        #print(part_ddls)

        return 'ALTER TABLE %s ' % table + 'ADD IF NOT EXISTS ' + ' '.join(part_ddls)


    def get_missing_partitions(self, table):
        msck_cmd = 'MSCK REPAIR TABLE %s ' % table
        #cmd = ' '.join([' '.join(self.hivecmd), '-e', command])
        #print(msck_cmd)
        msck_out = self.query(msck_cmd)
        missing_partitions = []
        #print(msck_out)
        for t in msck_out.split('\t'):
            if table in t:
                # missing_partitions.append({'location': t, 'parts': t.split(':')[1].split('/')})
                missing_partitions.append(t.split(':')[1])

        # re_compile = re.compile('%s:(.*) ' % table, re.MULTILINE)
        # missing_partitions = re.findall(re_compile, msck_out)
        #print(missing_partitions)
        return missing_partitions


    def get_partitions_definition(self, table):
        cmd = ' '.join(self.hivecmd) + ' -e \'SHOW CREATE TABLE ' + table + ';\' | sed -n \'/PARTITIONED BY (/,/)/p\' | grep -v \'PARTITIONED BY\' '
        partitions = sh(cmd).split(',')
        #print(partitions)
        pattern = re.compile('.*`(.+)`', re.MULTILINE)
        parts = []
        for pstr in partitions:
            match = re.search(pattern, pstr)
            part = match.group(1)
            parts.append(part)

        #print(parts)
        return parts


    def tables_get(self):
        """Returns a list of tables in the current database"""
        t = self.query('SHOW TABLES').split('\n')
        if 'tab_name' in t:
          t.remove('tab_name')
        return t


    def tables_init(self):
        """Initializes the self.tables dict"""

        for table in self.tables_get():
            self.tables[table] = {}

        return self.tables


    def table_exists(self, table): # ,force=False
        """Returns true if the table exists in the current database."""
        if not self.tables: self.tables_init()

        return table in self.tables.keys()


    def table_schema(self, table):
        if not self.tables: self.tables_init()

        if 'schema' not in self.tables[table].keys():
            cmd = ' '.join(self.hivecmd) + ' -e \'SHOW CREATE TABLE ' + table + ';\' | sed \'1d\''
            self.tables[table]['schema'] = sh(cmd)

        return self.tables[table]['schema']


    def table_location(self, table):
        if not self.tables: self.tables_init()

        if not 'location' in self.tables[table].keys():
            location_pattern = re.compile(r"^LOCATION\n\s+'(.+)'\n", re.MULTILINE)
            match = re.search(location_pattern, self.table_schema(table))
            location = match.group(1)
            self.tables[table]['location'] = location

        return self.tables[table]['location']

    def partitions(self, table):
        """Returns a list of partitions for the given Hive table."""
        if not self.tables: self.tables_init()

        # cache results for later
        # if we don't know the partitions yet, get them now
        if not 'partitions' in self.tables[table].keys():
            stdout     = self.query("SHOW PARTITIONS %s;" % table)
            partitions = stdout.split('\n')
            if 'partition' in partitions:
              partitions.remove('partition')
            self.tables[table]['partitions'] = partitions

        return self.tables[table]['partitions']


    def create_partitions(self, table, partition_datetimes):
        """
        Runs ALTER TABLE table ADD PARTITION ... for each of the partitions
        defined in the partition_datetimes list.
        """
        if partition_datetimes:
            q = self.add_partitions_ddl(table, partition_datetimes)
            # This query could be large if there are many partiitons to create.
            # Use a tempfile when adding partitions.
            return self.query(q, use_tempfile=True)
        else:
            logger.info("Not creating any partitions for table %s.  No partition datetimes were given." % table)


    def add_partitions_ddl(self, table, partition_datetimes):
        """
        Returns a complete hive statement to add partitions to
        table for the given datetimes.
        """
        ddls = [(self.partition_ddl(table, p)) for p in partition_datetimes]
        ddls.sort()
        return '\n'.join(['ALTER TABLE %s ADD %s;' % (table, ddl) for ddl in ddls])


    def partition_ddl(self, table, partition_datetime):
        """
        Returns a portion of a partition DDL statement.
        This statement is not enough to add a partition on its own,
        but may be used as part of a larger ALTER TABLE ADD ... statement.
        """
        interval = self.partition_interval(table)
        return 'PARTITION (%s) LOCATION \'%s\'' % (
            partition_datetime.strftime(interval_hierarchies[interval]['hive_partition_ddl_format']),
            self.partition_location(table, partition_datetime)
        )

    def partition_location(self, table, partition_datetime):
        interval = self.partition_interval(table)
        return self.table_location(table) + '/' + partition_datetime.strftime(interval_hierarchies[interval]['directory_format'])


    def partition_interval(self, table):
        """
        Examines the CREATE TABLE statements for partition layout.
        Determines the time interval of the partitions based on
        the number of partition levels.
        This will cache the interval result in the self.tables dict.
        """

        if not self.tables: self.tables_init()

        intervals = {
            4: 'hourly',
            3: 'daily',
            2: 'monthly',
            1: 'yearly',
        }

        # cache results for later
        if not 'interval' in self.tables[table].keys():

            # TODO: Use self.table_schema and a regex
            # rather than running SHOW CREATE TABLE AGAIN!

            # counts the number of partition keys this table has
            # and returns a string time interval based in this number
            cmd = ' '.join(self.hivecmd) + ' -e \'SHOW CREATE TABLE ' + table + ';\' | sed -n \'/PARTITIONED BY (/,/)/p\' | grep -v \'PARTITIONED BY\' | wc -l'
            partition_depth = int(sh(cmd))

            self.tables[table]['depth']    = partition_depth
            self.tables[table]['interval'] = intervals[partition_depth]

        return self.tables[table]['interval']

    def query(self, query, check_return_code=True, use_tempfile=False):
        """
        Runs the given Hive query and returns stdout.

        If use_tempfile is True, the query will be written to
        a temporary file and run as a Hive script.
        """

        if use_tempfile:
            f = tempfile.NamedTemporaryFile(prefix='tmp-hive-query-', suffix='.hiveql')
            logger.debug('Writing Hive query to tempfile %s.' % f.name)
            f.write(query)
            f.flush()
            out = self.script(f.name)
            # NamedTemporaryFile will be deleted on close().
            f.close()
            return out
        else:
            return self.command(['-e', query], check_return_code)


    def script(self, script, check_return_code=True):
        """Runs the contents of the given script in hive and returns stdout."""
        if not os.path.isfile(script):
            raise RuntimeError("Hive script: {0} does not exist.".format(script))
        return self.command( ['-f', script], check_return_code)


    def command(self, args, check_return_code=True):
        """Runs the `hive` from the command line, passing in the given args, and
           returning stdout.
        """
        cmd = self.hivecmd + args
        return sh(cmd, check_return_code)


class HdfsDatasetUtils(object):
    """
    Deal with time partitioned data imports in HDFS.
    Data directory hierarchies must be of the form:
      <datadir>/<dataset>/<interval>/<year>/<month>/<day>/<hour>.

    <datadir> is the HDFS path of all of your data imports.
    <dataset> is the name of the dataset inside of <datadir>

    Intervals supported:
      hourly
      monthly
      daily
      yearly

    The depth of the hierarchy is dependent on the <interval>.
    E.g. monthly will only be 2 directories deep (<year>/<month>), etc.

    This is useful for identifying time buckets created by Camus,
    LinkedIn's Kafka -> HDFS import framework.
    """
    def __init__(self, datadir):
        self.datadir = datadir

    def datasets(self):
        """Reads dataset names out of <datadir>"""
        return sh(
            'hdfs dfs -ls {0}/ | '\
            'grep -v "Found .* items" | '\
            'awk -F "/" \'{{print $NF}}\''.format(
                self.datadir
            )
        ).split('\n')

    def dataset_dir(self, dataset, interval='hourly'):
        return (self.datadir + '/' + dataset + '/' + interval)

    # TODO configure interval partition paths automatically instead of hardcoding with time buckets
    def partitions(self, dataset, interval='hourly'):
        """
        Returns a list of time bucketed 'partitions' in HDFS for this dataset.
        These are inferred from the directory hierarchy in HDFS.
        """

        basedir = self.dataset_dir(dataset, interval)

        # number of  wildcard time bucket directories, e.g. depth_stars == 4 -> '/*/*/*/*'
        depth_stars = '/*' * interval_hierarchies[interval]['depth']
        directories = sh(
            'hdfs dfs -ls -d {0}{1} | '\
            'grep -v \'Found .* items\' | '\
            'awk \'{{print $NF}}\''.format(basedir, depth_stars)
        ).split('\n')

        # return list of time bucket directories, e.g. ['2013/10/09/15', '2013/10/09/16', ... ]
        return [directory.replace(basedir + '/', '') for directory in directories]
